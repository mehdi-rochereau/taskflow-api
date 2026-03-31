package com.mehdi.taskflow.project.dto;

import com.mehdi.taskflow.project.Project;

import java.time.LocalDateTime;

public class ProjectResponse {

    private Long id;
    private String name;
    private String description;
    private String ownerUsername;
    private LocalDateTime createdAt;

    public ProjectResponse(Project project) {
        this.id = project.getId();
        this.name = project.getName();
        this.description = project.getDescription();
        this.ownerUsername = project.getOwner().getUsername();
        this.createdAt = project.getCreatedAt();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getOwnerUsername() { return ownerUsername; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}