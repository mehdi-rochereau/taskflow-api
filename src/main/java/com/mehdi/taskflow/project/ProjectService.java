package com.mehdi.taskflow.project;

import com.mehdi.taskflow.exception.ResourceNotFoundException;
import com.mehdi.taskflow.project.dto.ProjectRequest;
import com.mehdi.taskflow.user.User;
import com.mehdi.taskflow.user.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository,
                          UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    @PreAuthorize("isAuthenticated()")
    public List<Project> getMyProjects() {
        User currentUser = getCurrentUser();
        return projectRepository.findByOwnerId(currentUser.getId());
    }

    @PreAuthorize("isAuthenticated()")
    public Project createProject(ProjectRequest request) {
        User currentUser = getCurrentUser();

        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setOwner(currentUser);

        return projectRepository.save(project);
    }

    @PreAuthorize("isAuthenticated()")
    public Project updateProject(Long id, ProjectRequest request) {
        User currentUser = getCurrentUser();

        if (!projectRepository.existsByIdAndOwnerId(id, currentUser.getId())) {
            throw new RuntimeException("Accès refusé");
        }

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable"));

        project.setName(request.getName());
        project.setDescription(request.getDescription());

        return projectRepository.save(project);
    }

    @PreAuthorize("isAuthenticated()")
    public void deleteProject(Long id) {
        User currentUser = getCurrentUser();

        if (!projectRepository.existsByIdAndOwnerId(id, currentUser.getId())) {
            throw new RuntimeException("Accès refusé");
        }

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable"));

        projectRepository.delete(project);
    }
}