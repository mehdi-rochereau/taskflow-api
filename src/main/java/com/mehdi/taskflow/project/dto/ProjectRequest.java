package com.mehdi.taskflow.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO carrying project creation and update data.
 *
 * <p>Used as the request body for:
 * <ul>
 *   <li>{@code POST /api/projects} — create a new project</li>
 *   <li>{@code PUT /api/projects/{id}} — update an existing project</li>
 * </ul>
 * </p>
 *
 * @see com.mehdi.taskflow.project.ProjectController
 * @see com.mehdi.taskflow.project.ProjectService
 */
public class ProjectRequest {

    /**
     * Project name. Required, 100 characters maximum.
     */
    @NotBlank(message = "Le nom du projet est obligatoire")
    @Size(max = 100)
    private String name;

    /**
     * Optional project description. 500 characters maximum.
     */
    @Size(max = 500)
    private String description;

    /**
     * Default constructor required for JSON deserialization.
     */
    public ProjectRequest() {}

    /** @return the project name */
    public String getName() { return name; }

    /** @param name the project name */
    public void setName(String name) { this.name = name; }

    /** @return the optional project description */
    public String getDescription() { return description; }

    /** @param description the optional project description */
    public void setDescription(String description) { this.description = description; }
}