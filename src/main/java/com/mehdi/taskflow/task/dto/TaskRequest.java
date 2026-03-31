package com.mehdi.taskflow.task.dto;

import com.mehdi.taskflow.task.TaskPriority;
import com.mehdi.taskflow.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class TaskRequest {

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 200)
    private String title;

    @Size(max = 1000)
    private String description;

    @NotNull(message = "Le statut est obligatoire")
    private TaskStatus status;

    @NotNull(message = "La priorité est obligatoire")
    private TaskPriority priority;

    private LocalDate dueDate;

    private Long assigneeId;

    public TaskRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
}