package com.mehdi.taskflow.project;

import com.mehdi.taskflow.exception.ResourceNotFoundException;
import com.mehdi.taskflow.project.dto.ProjectRequest;
import com.mehdi.taskflow.security.SecurityUtils;
import com.mehdi.taskflow.user.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service handling project management operations.
 *
 * <p>All operations are restricted to the currently authenticated user.
 * Ownership is enforced on every mutating operation — only the project owner
 * can update or delete a project.</p>
 *
 * <p>The authenticated user is resolved via {@link SecurityUtils},
 * which reads the current {@link org.springframework.security.core.context.SecurityContext}.</p>
 *
 * @see SecurityUtils
 * @see ProjectRepository
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final SecurityUtils securityUtils;

    /**
     * Constructs a new {@code ProjectService} with its required dependencies.
     *
     * @param projectRepository repository for project persistence
     * @param securityUtils     utility for resolving the currently authenticated user
     */
    public ProjectService(ProjectRepository projectRepository,
                          SecurityUtils securityUtils) {
        this.projectRepository = projectRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * Returns all projects owned by the currently authenticated user.
     *
     * @return list of projects belonging to the current user, empty if none exist
     */
    @PreAuthorize("isAuthenticated()")
    public List<Project> getMyProjects() {
        User currentUser = securityUtils.getCurrentUser();
        return projectRepository.findByOwnerId(currentUser.getId());
    }

    /**
     * Returns a project by its identifier.
     *
     * <p>Access is restricted to the project owner.
     * If the current user is not the owner, access is denied.</p>
     *
     * @param id the project identifier
     * @return the matching project
     * @throws ResourceNotFoundException if no project exists with the given id
     * @throws AccessDeniedException     if the current user is not the project owner
     */
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

    /**
     * Creates a new project owned by the currently authenticated user.
     *
     * @param request data for the project to create
     * @return the persisted project with its generated identifier
     */
    @PreAuthorize("isAuthenticated()")
    public Project createProject(ProjectRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setOwner(currentUser);
        return projectRepository.save(project);
    }

    /**
     * Updates an existing project.
     *
     * <p>Ownership is verified before loading the project —
     * if the current user is not the owner, the project is never fetched.</p>
     *
     * @param id      the identifier of the project to update
     * @param request updated project data
     * @return the updated project
     * @throws AccessDeniedException     if the current user is not the project owner
     * @throws ResourceNotFoundException if no project exists with the given id
     */
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

    /**
     * Permanently deletes a project.
     *
     * <p>Ownership is verified before loading the project —
     * if the current user is not the owner, the project is never fetched.</p>
     *
     * @param id the identifier of the project to delete
     * @throws AccessDeniedException     if the current user is not the project owner
     * @throws ResourceNotFoundException if no project exists with the given id
     */
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