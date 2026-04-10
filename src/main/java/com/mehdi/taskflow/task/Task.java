package com.mehdi.taskflow.task;

import com.mehdi.taskflow.project.Project;
import com.mehdi.taskflow.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a task within a project.
 *
 * <p>A task belongs to exactly one {@link Project} and can optionally
 * be assigned to a {@link User}. Tasks support status and priority tracking
 * via the {@link TaskStatus} and {@link TaskPriority} enumerations.</p>
 *
 * <p>Both {@link #project} and {@link #assignee} associations are loaded lazily
 * to avoid unnecessary database queries.</p>
 *
 * @see Project
 * @see User
 * @see TaskStatus
 * @see TaskPriority
 */
@Entity
@Table(name = "tasks")
public class Task {

    /**
     * Auto-generated primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The project this task belongs to.
     * Loaded lazily — only fetched when explicitly accessed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /**
     * The user assigned to this task. Optional — a task may be unassigned.
     * Loaded lazily — only fetched when explicitly accessed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    /**
     * Task title. Required, 200 characters maximum.
     */
    @NotBlank
    @Size(max = 200)
    @Column(nullable = false)
    private String title;

    /**
     * Optional task description. 1000 characters maximum.
     */
    @Size(max = 1000)
    private String description;

    /**
     * Current status of the task. Defaults to {@link TaskStatus#TODO}.
     * Stored as a string in the database for readability.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.TODO;

    /**
     * Priority level of the task. Defaults to {@link TaskPriority#MEDIUM}.
     * Stored as a string in the database for readability.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority = TaskPriority.MEDIUM;

    /**
     * Optional due date for the task.
     */
    private LocalDate dueDate;

    /**
     * Timestamp of task creation. Set automatically on first persist.
     * Cannot be updated after creation.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Default constructor required by JPA.
     */
    public Task() {}

    /**
     * Sets the creation timestamp before the entity is first persisted.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // --- Getters and setters ---

    /** @return the task's unique identifier */
    public Long getId() { return id; }

    /** @param id the task's unique identifier */
    public void setId(Long id) { this.id = id; }

    /** @return the project this task belongs to */
    public Project getProject() { return project; }

    /** @param project the project this task belongs to */
    public void setProject(Project project) { this.project = project; }

    /** @return the assigned user, or {@code null} if unassigned */
    public User getAssignee() { return assignee; }

    /** @param assignee the user to assign this task to, or {@code null} to unassign */
    public void setAssignee(User assignee) { this.assignee = assignee; }

    /** @return the task title */
    public String getTitle() { return title; }

    /** @param title the task title */
    public void setTitle(String title) { this.title = title; }

    /** @return the task description */
    public String getDescription() { return description; }

    /** @param description the task description */
    public void setDescription(String description) { this.description = description; }

    /** @return the current task status */
    public TaskStatus getStatus() { return status; }

    /** @param status the new task status */
    public void setStatus(TaskStatus status) { this.status = status; }

    /** @return the task priority */
    public TaskPriority getPriority() { return priority; }

    /** @param priority the task priority */
    public void setPriority(TaskPriority priority) { this.priority = priority; }

    /** @return the optional due date */
    public LocalDate getDueDate() { return dueDate; }

    /** @param dueDate the optional due date */
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    /** @return the task creation timestamp */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** @param createdAt the task creation timestamp */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}