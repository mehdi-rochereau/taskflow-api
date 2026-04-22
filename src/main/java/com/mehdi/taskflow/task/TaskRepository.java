package com.mehdi.taskflow.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link Task} entities.
 *
 * <p>Provides task lookup methods with eager fetching of related entities
 * ({@code project} and {@code assignee}) to avoid N+1 query issues
 * during JSON serialization.</p>
 *
 * <p>All query methods using {@code JOIN FETCH} load the associated
 * {@link com.mehdi.taskflow.project.Project} eagerly and the
 * {@link com.mehdi.taskflow.user.User} assignee with a {@code LEFT JOIN FETCH}
 * to handle unassigned tasks gracefully.</p>
 *
 * @see Task
 * @see TaskService
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * Returns all tasks belonging to a project, with their project and assignee eagerly loaded.
     *
     * @param projectId the project identifier
     * @return list of tasks for the given project, empty if none exist
     */
    @Query("SELECT t FROM Task t JOIN FETCH t.project LEFT JOIN FETCH t.assignee WHERE t.project.id = :projectId")
    List<Task> findByProjectId(@Param("projectId") Long projectId);

    /**
     * Returns all tasks belonging to a project filtered by status,
     * with their project and assignee eagerly loaded.
     *
     * @param projectId the project identifier
     * @param status    the task status to filter by
     * @return list of matching tasks, empty if none exist
     */
    @Query("SELECT t FROM Task t JOIN FETCH t.project LEFT JOIN FETCH t.assignee WHERE t.project.id = :projectId AND t.status = :status")
    List<Task> findByProjectIdAndStatus(@Param("projectId") Long projectId, @Param("status") TaskStatus status);

    /**
     * Returns all tasks belonging to a project filtered by priority,
     * with their project and assignee eagerly loaded.
     *
     * @param projectId the project identifier
     * @param priority  the task priority to filter by
     * @return list of matching tasks, empty if none exist
     */
    @Query("SELECT t FROM Task t JOIN FETCH t.project LEFT JOIN FETCH t.assignee WHERE t.project.id = :projectId AND t.priority = :priority")
    List<Task> findByProjectIdAndPriority(@Param("projectId") Long projectId, @Param("priority") TaskPriority priority);

    /**
     * Returns all tasks assigned to a specific user.
     *
     * @param assigneeId the assignee's user identifier
     * @return list of tasks assigned to the given user, empty if none exist
     */
    List<Task> findByAssigneeId(Long assigneeId);

    /**
     * Checks whether a task exists and belongs to a project owned by the given user.
     *
     * <p>Used to verify ownership before performing mutating operations
     * (update, delete) without loading the full task entity.</p>
     *
     * @param id      the task identifier
     * @param ownerId the project owner's user identifier
     * @return {@code true} if the task exists and the user owns its project
     */
    boolean existsByIdAndProjectOwnerId(Long id, Long ownerId);
}