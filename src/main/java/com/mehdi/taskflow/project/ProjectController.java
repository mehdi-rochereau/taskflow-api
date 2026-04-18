package com.mehdi.taskflow.project;

import com.mehdi.taskflow.project.dto.ProjectRequest;
import com.mehdi.taskflow.project.dto.ProjectResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller handling project management operations.
 *
 * <p>All endpoints require a valid JWT token passed as a
 * {@code Authorization: Bearer <token>} header.
 * Operations are automatically scoped to the authenticated user —
 * only projects owned by the current user are accessible.</p>
 *
 * <p>All responses are produced in {@code application/json} format.</p>
 *
 * @see ProjectService
 */
@RestController
@RequestMapping(value = "/api/projects", produces = "application/json")
public class ProjectController implements ProjectControllerApi {

    private final ProjectService projectService;

    /**
     * Constructs a new {@code ProjectController} with its required dependency.
     *
     * @param projectService service handling project business logic
     */
    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * Returns all projects owned by the authenticated user.
     *
     * @return {@code 200 OK} with the list of projects, empty array if none exist,
     * or {@code 401 Unauthorized} if the JWT token is missing or invalid
     */
    @Override
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getMyProjects() {
        return ResponseEntity.ok(
                projectService.getMyProjects().stream()
                        .map(ProjectResponse::new)
                        .toList()
        );
    }

    /**
     * Returns a project by its identifier.
     *
     * <p>Access is restricted to the project owner.</p>
     *
     * @param id the project identifier
     * @return {@code 200 OK} with the project details,
     * {@code 401 Unauthorized} if the JWT token is missing or invalid,
     * {@code 403 Forbidden} if the project belongs to another user,
     * or {@code 404 Not Found} if no project exists with the given id
     */
    @Override
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long id) {
        return ResponseEntity.ok(new ProjectResponse(projectService.getProjectById(id)));
    }

    /**
     * Creates a new project for the authenticated user.
     *
     * @param request the project data — name and optional description
     * @return {@code 201 Created} with the created project,
     * {@code 400 Bad Request} if validation fails,
     * or {@code 401 Unauthorized} if the JWT token is missing or invalid
     */
    @Override
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ProjectResponse(projectService.createProject(request)));
    }

    /**
     * Updates an existing project.
     *
     * <p>Only the project owner can perform this operation.</p>
     *
     * @param id      the identifier of the project to update
     * @param request the updated project data — name and optional description
     * @return {@code 200 OK} with the updated project,
     * {@code 400 Bad Request} if validation fails,
     * {@code 401 Unauthorized} if the JWT token is missing or invalid,
     * {@code 403 Forbidden} if the project belongs to another user,
     * or {@code 404 Not Found} if no project exists with the given id
     */
    @Override
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable Long id,
                                                         @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.ok(new ProjectResponse(projectService.updateProject(id, request)));
    }

    /**
     * Permanently deletes a project.
     *
     * <p>Only the project owner can perform this operation.</p>
     *
     * @param id the identifier of the project to delete
     * @return {@code 204 No Content} on success,
     * {@code 401 Unauthorized} if the JWT token is missing or invalid,
     * {@code 403 Forbidden} if the project belongs to another user,
     * or {@code 404 Not Found} if no project exists with the given id
     */
    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}