package com.mehdi.taskflow.task.dto;

import com.mehdi.taskflow.task.Task;
import com.mehdi.taskflow.task.TaskPriority;
import com.mehdi.taskflow.task.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDate dueDate;
    private Long projectId;
    private String assigneeUsername;
    private LocalDateTime createdAt;

    public TaskResponse(Task task) {
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

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TaskStatus getStatus() { return status; }
    public TaskPriority getPriority() { return priority; }
    public LocalDate getDueDate() { return dueDate; }
    public Long getProjectId() { return projectId; }
    public String getAssigneeUsername() { return assigneeUsername; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}