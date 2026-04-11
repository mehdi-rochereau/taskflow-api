package com.mehdi.taskflow.project;

import com.mehdi.taskflow.exception.ResourceNotFoundException;
import com.mehdi.taskflow.project.dto.ProjectRequest;
import com.mehdi.taskflow.security.SecurityUtils;
import com.mehdi.taskflow.user.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final SecurityUtils securityUtils;

    public ProjectService(ProjectRepository projectRepository,
                          SecurityUtils securityUtils) {
        this.projectRepository = projectRepository;
        this.securityUtils = securityUtils;
    }

    @PreAuthorize("isAuthenticated()")
    public List<Project> getMyProjects() {
        User currentUser = securityUtils.getCurrentUser();
        return projectRepository.findByOwnerId(currentUser.getId());
    }

    @PreAuthorize("isAuthenticated()")
    public Project getProjectById(Long id) {
        User currentUser = securityUtils.getCurrentUser();
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable"));
        if (!project.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Accès refusé");
        }
        return project;
    }

    @PreAuthorize("isAuthenticated()")
    public Project createProject(ProjectRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setOwner(currentUser);
        return projectRepository.save(project);
    }

    @PreAuthorize("isAuthenticated()")
    public Project updateProject(Long id, ProjectRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        if (!projectRepository.existsByIdAndOwnerId(id, currentUser.getId())) {
            throw new AccessDeniedException("Accès refusé");
        }
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable"));
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        return projectRepository.save(project);
    }

    @PreAuthorize("isAuthenticated()")
    public void deleteProject(Long id) {
        User currentUser = securityUtils.getCurrentUser();
        if (!projectRepository.existsByIdAndOwnerId(id, currentUser.getId())) {
            throw new AccessDeniedException("Accès refusé");
        }
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable"));
        projectRepository.delete(project);
    }
}