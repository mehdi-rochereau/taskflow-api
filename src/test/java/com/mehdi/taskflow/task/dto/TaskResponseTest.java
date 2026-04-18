package com.mehdi.taskflow.task.dto;

import com.mehdi.taskflow.project.Project;
import com.mehdi.taskflow.task.Task;
import com.mehdi.taskflow.task.TaskPriority;
import com.mehdi.taskflow.task.TaskStatus;
import com.mehdi.taskflow.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TaskResponseTest {

    private Task task;

    @BeforeEach
    void setUp() {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("mehdi");

        Project project = new Project();
        project.setId(1L);
        project.setOwner(owner);

        task = new Task();
        task.setId(1L);
        task.setTitle("My task");
        task.setDescription("Description");
        task.setStatus(TaskStatus.TODO);
        task.setPriority(TaskPriority.MEDIUM);
        task.setDueDate(LocalDate.of(2026, 12, 31));
        task.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        task.setProject(project);
    }

    @Test
    void constructor_shouldMapAllFields_whenAssigneeIsPresent() {
        // GIVEN
        User assignee = new User();
        assignee.setId(2L);
        assignee.setUsername("assignee");
        task.setAssignee(assignee);

        // WHEN
        TaskResponse response = new TaskResponse(task);

        // THEN
        assertEquals(1L, response.getId());
        assertEquals("My task", response.getTitle());
        assertEquals("Description", response.getDescription());
        assertEquals(TaskStatus.TODO, response.getStatus());
        assertEquals(TaskPriority.MEDIUM, response.getPriority());
        assertEquals(LocalDate.of(2026, 12, 31), response.getDueDate());
        assertEquals(1L, response.getProjectId());
        assertEquals("assignee", response.getAssigneeUsername());
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), response.getCreatedAt());
    }

    @Test
    void constructor_shouldSetAssigneeUsernameToNull_whenAssigneeIsAbsent() {
        // GIVEN
        task.setAssignee(null);

        // WHEN
        TaskResponse response = new TaskResponse(task);

        // THEN
        assertNull(response.getAssigneeUsername());
        assertEquals(1L, response.getId());
        assertEquals("My task", response.getTitle());
    }

    @Test
    void constructor_shouldThrow_whenTaskIsNull() {
        // WHEN & THEN
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> new TaskResponse(null));
        assertEquals("Task must not be null", ex.getMessage());
    }
}