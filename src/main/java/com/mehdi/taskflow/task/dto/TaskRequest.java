package com.mehdi.taskflow.task.dto;

import com.mehdi.taskflow.task.TaskPriority;
import com.mehdi.taskflow.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO carrying task creation and update data.
 *
 * <p>Used as the request body for:
 * <ul>
 *   <li>{@code POST /api/projects/{projectId}/tasks} — create a new task</li>
 *   <li>{@code PUT /api/projects/{projectId}/tasks/{id}} — update an existing task</li>
 * </ul>
 * </p>
 *
 * @see com.mehdi.taskflow.task.TaskController
 * @see com.mehdi.taskflow.task.TaskService
 */
public class TaskRequest {

    /**
     * Task title. Required, 200 characters maximum.
     */
    @NotBlank(message = "{validation.task.title.required}")
    @Size(max = 200, message = "{validation.task.title.size}")
    private String title;

    /**
     * Optional task description. 1000 characters maximum.
     */
    @Size(max = 1000, message = "{validation.task.description.size}")
    private String description;

    /**
     * Task status. Required — must be one of {@link TaskStatus#TODO},
     * {@link TaskStatus#IN_PROGRESS}, or {@link TaskStatus#DONE}.
     */
    @NotNull(message = "{validation.task.status.required}")
    private TaskStatus status;

    /**
     * Task priority. Required — must be one of {@link TaskPriority#LOW},
     * {@link TaskPriority#MEDIUM}, or {@link TaskPriority#HIGH}.
     */
    @NotNull(message = "{validation.task.priority.required}")
    private TaskPriority priority;

    /**
     * Optional due date for the task.
     */
    private LocalDate dueDate;

    /**
     * Optional identifier of the user to assign this task to.
     * If {@code null}, the task remains unassigned.
     * Must reference an existing user — validated by the service layer.
     */
    private Long assigneeId;

    /**
     * Default constructor required for JSON deserialization.
     */
    public TaskRequest() {}

    /** @return the task title */
    public String getTitle() { return title; }

    /** @param title the task title */
    public void setTitle(String title) { this.title = title; }

    /** @return the optional task description */
    public String getDescription() { return description; }

    /** @param description the optional task description */
    public void setDescription(String description) { this.description = description; }

    /** @return the task status */
    public TaskStatus getStatus() { return status; }

    /** @param status the task status */
    public void setStatus(TaskStatus status) { this.status = status; }

    /** @return the task priority */
    public TaskPriority getPriority() { return priority; }

    /** @param priority the task priority */
    public void setPriority(TaskPriority priority) { this.priority = priority; }

    /** @return the optional due date */
    public LocalDate getDueDate() { return dueDate; }

    /** @param dueDate the optional due date */
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    /** @return the optional assignee identifier, or {@code null} if unassigned */
    public Long getAssigneeId() { return assigneeId; }

    /** @param assigneeId the optional assignee identifier */
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
}