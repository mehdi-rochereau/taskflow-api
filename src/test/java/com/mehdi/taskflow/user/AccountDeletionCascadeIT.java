package com.mehdi.taskflow.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

import com.mehdi.taskflow.AbstractIntegrationTest;
import com.mehdi.taskflow.project.Project;
import com.mehdi.taskflow.task.Task;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/**
 * Verifies the referential integrity rules enforced by migration V5 on user deletion.
 *
 * <p>These rules live in the database schema, not in Java: no {@code @OneToMany} association is
 * declared on {@link User}, so nothing in the persistence layer compensates for a missing {@code ON
 * DELETE} clause. Only a real engine can prove the behaviour, which is why this test runs against a
 * MySQL container rather than a mock.
 *
 * <p>Two distinct rules are covered: owned projects and their tasks are deleted with their owner,
 * while tasks merely assigned to the deleted user survive inside the project they belong to.
 */
@DataJpaTest
// The container is the database under test: NONE prevents Spring Boot from
// swapping the datasource for an embedded one, which is its default behaviour
// under @DataJpaTest and would silently defeat the whole point of this class.
@AutoConfigureTestDatabase(replace = NONE)
@DisplayName("User deletion cascade rules (V5)")
class AccountDeletionCascadeIT extends AbstractIntegrationTest {

    @Autowired private TestEntityManager entityManager;

    /**
     * Persists a user with the minimum set of non-null fields required by the schema. The creation
     * timestamp is set by the {@code @PrePersist} callback on {@link User}, so it is deliberately
     * left alone here.
     *
     * @param username the unique username, also used to derive the email
     * @return the persisted user, with its generated identifier populated
     */
    private User persistUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        // Not a real hash: no authentication happens in this slice, and using a
        // BCrypt encoder here would drag a security bean into a persistence test.
        user.setPassword("irrelevant-for-this-slice");
        user.setRole("ROLE_USER");
        return entityManager.persist(user);
    }

    /**
     * Persists a project owned by the given user.
     *
     * @param name the project name
     * @param owner the owning user, already persisted
     * @return the persisted project, with its generated identifier populated
     */
    private Project persistProject(String name, User owner) {
        Project project = new Project();
        project.setName(name);
        project.setOwner(owner);
        return entityManager.persist(project);
    }

    /**
     * Persists a task inside the given project, optionally assigned to a user.
     *
     * @param title the task title
     * @param project the owning project, already persisted
     * @param assignee the assigned user, or {@code null} to leave it unassigned
     * @return the persisted task, with its generated identifier populated
     */
    private Task persistTask(String title, Project project, User assignee) {
        Task task = new Task();
        task.setTitle(title);
        task.setProject(project);
        task.setAssignee(assignee);
        return entityManager.persist(task);
    }

    /**
     * Counts rows matching an identifier, bypassing the persistence context.
     *
     * <p>A JPA {@code find} would be answered from the first-level cache and would report entities
     * that the database has already cascaded away, since Hibernate is unaware of engine-level
     * deletions. A native query always reaches the database.
     *
     * @param table the table name
     * @param column the column to filter on
     * @param value the identifier to match
     * @return the number of matching rows
     */
    private long countRows(String table, String column, Long value) {
        Object result =
                entityManager
                        .getEntityManager()
                        .createNativeQuery(
                                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = :value")
                        .setParameter("value", value)
                        .getSingleResult();
        return ((Number) result).longValue();
    }

    @Test
    @DisplayName("deleting a user removes the projects they own and the tasks inside them")
    void deletingUserCascadesToOwnedProjectsAndTheirTasks() {
        User owner = persistUser("owner");
        Project project = persistProject("Owned project", owner);
        persistTask("First task", project, null);
        persistTask("Second task", project, owner);

        Long ownerId = owner.getId();
        Long projectId = project.getId();

        // Everything is written and detached before the deletion. Without this,
        // the project and tasks still held in the persistence context would
        // reference a User that Hibernate considers removed, and the flush below
        // would fail with a TransientObjectException: Hibernate has no way to
        // know the engine will cascade the deletion for it.
        entityManager.flush();
        entityManager.clear();

        User managedOwner = entityManager.find(User.class, ownerId);
        entityManager.remove(managedOwner);
        // flush sends the DELETE to MySQL, which is what triggers the engine-level
        // cascade. Without it Hibernate would keep the statement pending until the
        // end of the transaction and the assertions below would read a stale state.
        entityManager.flush();
        // clear detaches everything so that later reads hit the database instead of
        // the first-level cache, which still holds the cascaded entities.
        entityManager.clear();

        assertThat(countRows("users", "id", ownerId)).isZero();
        assertThat(countRows("projects", "id", projectId)).isZero();
        assertThat(countRows("tasks", "project_id", projectId)).isZero();
    }

    @Test
    @DisplayName(
            "deleting an assignee keeps tasks owned by a third party project and unassigns them")
    void deletingAssigneeSetsTaskAssigneeToNull() {
        User owner = persistUser("project-owner");
        User assignee = persistUser("assignee");
        Project project = persistProject("Third party project", owner);
        Task task = persistTask("Assigned task", project, assignee);

        Long assigneeId = assignee.getId();
        Long projectId = project.getId();
        Long taskId = task.getId();

        entityManager.flush();
        entityManager.clear();

        User managedAssignee = entityManager.find(User.class, assigneeId);
        entityManager.remove(managedAssignee);
        entityManager.flush();
        entityManager.clear();

        assertThat(countRows("users", "id", assigneeId)).isZero();
        // The task itself must survive: it belongs to its project, not to its assignee.
        assertThat(countRows("tasks", "id", taskId)).isEqualTo(1);
        // And its assignee_id must have been nulled by ON DELETE SET NULL rather
        // than left dangling, which the foreign key would have rejected anyway.
        assertThat(countRows("tasks", "assignee_id", assigneeId)).isZero();

        Task reloaded = entityManager.find(Task.class, taskId);
        assertThat(reloaded.getAssignee()).isNull();
        assertThat(reloaded.getProject().getId()).isEqualTo(projectId);
    }
}
