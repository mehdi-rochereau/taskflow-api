package com.mehdi.taskflow.task.dto;

import com.mehdi.taskflow.task.Task;
import com.mehdi.taskflow.task.TaskPriority;
import com.mehdi.taskflow.task.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Read-only DTO representing a task in API responses.
 *
 * <p>Wraps a {@link Task} entity and exposes only the fields
 * relevant to the API consumer. The full project and assignee entities
 * are replaced by their respective identifiers and usernames.</p>
 *
 * <p>This class is immutable — all fields are final and set once
 * at construction time. No setters are provided.</p>
 *
 * <p>Used as the response body for all task endpoints.</p>
 *
 * @see com.mehdi.taskflow.task.TaskController
 * @see Task
 */
public class TaskResponse {

    /** Unique task identifier. */
    private final Long id;

    /** Task title. */
    private final String title;

    /** Optional task description. */
    private final String description;

    /** Current task status. */
    private final TaskStatus status;

    /** Task priority level. */
    private final TaskPriority priority;

    /** Optional due date. */
    private final LocalDate dueDate;

    /** Identifier of the project this task belongs to. */
    private final Long projectId;

    /**
     * Username of the assigned user, or {@code null} if unassigned.
     */
    private final String assigneeUsername;

    /** Timestamp of task creation. */
    private final LocalDateTime createdAt;

    /**
     * Constructs a {@code TaskResponse} from a {@link Task} entity.
     *
     * @param task the task entity to map — must not be {@code null}
     * @throws NullPointerException if {@code task} is {@code null}
     */
    public TaskResponse(Task task) {
        Objects.requireNonNull(task, "Task must not be null");
        this.id = task.getId();
        this.title = task.getTitle();
        this.description = task.getDescription();
        this.status = task.getStatus();
        this.priority = task.getPriority();
        this.dueDate = task.getDueDate();
        this.projectId = task.getProject().getId();
        this.assigneeUsername = task.getAssignee() != null
                ? task.getAssignee().getUsername()
                : null;
        this.createdAt = task.getCreatedAt();
    }

    /** @return the task's unique identifier */
    public Long getId() { return id; }

    /** @return the task title */
    public String getTitle() { return title; }

    /** @return the optional task description */
    public String getDescription() { return description; }

    /** @return the current task status */
    public TaskStatus getStatus() { return status; }

    /** @return the task priority */
    public TaskPriority getPriority() { return priority; }

    /** @return the optional due date */
    public LocalDate getDueDate() { return dueDate; }

    /** @return the identifier of the project this task belongs to */
    public Long getProjectId() { return projectId; }

    /** @return the assignee's username, or {@code null} if unassigned */
    public String getAssigneeUsername() { return assigneeUsername; }

    /** @return the task creation timestamp */
    public LocalDateTime getCreatedAt() { return createdAt; }
}