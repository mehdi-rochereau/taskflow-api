package com.mehdi.taskflow.project.dto;

import com.mehdi.taskflow.project.Project;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Read-only DTO representing a project in API responses.
 *
 * <p>Wraps a {@link Project} entity and exposes only the fields
 * relevant to the API consumer. Sensitive or internal fields
 * (such as the full owner entity) are excluded.</p>
 *
 * <p>This class is immutable — all fields are final and set once
 * at construction time. No setters are provided.</p>
 *
 * <p>Used as the response body for all project endpoints.</p>
 *
 * @see com.mehdi.taskflow.project.ProjectController
 * @see Project
 */
public class ProjectResponse {

    /** Unique project identifier. */
    private final Long id;

    /** Project name. */
    private final String name;

    /** Optional project description. */
    private final String description;

    /** Username of the project owner. */
    private final String ownerUsername;

    /** Timestamp of project creation. */
    private final LocalDateTime createdAt;

    /**
     * Constructs a {@code ProjectResponse} from a {@link Project} entity.
     *
     * @param project the project entity to map — must not be {@code null}
     * @throws NullPointerException if {@code project} is {@code null}
     */
    public ProjectResponse(Project project) {
        Objects.requireNonNull(project, "Project must not be null");
        this.id = project.getId();
        this.name = project.getName();
        this.description = project.getDescription();
        this.ownerUsername = project.getOwner().getUsername();
        this.createdAt = project.getCreatedAt();
    }

    /** @return the project's unique identifier */
    public Long getId() { return id; }

    /** @return the project name */
    public String getName() { return name; }

    /** @return the optional project description */
    public String getDescription() { return description; }

    /** @return the username of the project owner */
    public String getOwnerUsername() { return ownerUsername; }

    /** @return the project creation timestamp */
    public LocalDateTime getCreatedAt() { return createdAt; }
}