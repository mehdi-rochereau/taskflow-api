package com.mehdi.taskflow.project;

import com.mehdi.taskflow.task.Task;
import com.mehdi.taskflow.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Entity representing a project owned by a user.
 *
 * <p>A project acts as a container for {@link Task} entities.
 * Only the owner can create, update, or delete tasks within the project.</p>
 *
 * <p>The {@link #owner} association is loaded lazily to avoid unnecessary
 * database queries when only project metadata is needed.</p>
 *
 * @see Task
 * @see User
 */
@Entity
@Table(name = "projects")
public class Project {

    /**
     * Auto-generated primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who owns this project.
     * Loaded lazily — only fetched when explicitly accessed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /**
     * Project name. Required, 100 characters maximum.
     */
    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String name;

    /**
     * Optional project description. 500 characters maximum.
     */
    @Size(max = 500)
    private String description;

    /**
     * Timestamp of project creation. Set automatically on first persist.
     * Cannot be updated after creation.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Default constructor required by JPA.
     */
    public Project() {}

    /**
     * Sets the creation timestamp before the entity is first persisted.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // --- Getters and setters ---

    /** @return the project's unique identifier */
    public Long getId() { return id; }

    /** @param id the project's unique identifier */
    public void setId(Long id) { this.id = id; }

    /** @return the project owner */
    public User getOwner() { return owner; }

    /** @param owner the project owner */
    public void setOwner(User owner) { this.owner = owner; }

    /** @return the project name */
    public String getName() { return name; }

    /** @param name the project name */
    public void setName(String name) { this.name = name; }

    /** @return the project description */
    public String getDescription() { return description; }

    /** @param description the project description */
    public void setDescription(String description) { this.description = description; }

    /** @return the project creation timestamp */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** @param createdAt the project creation timestamp */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}