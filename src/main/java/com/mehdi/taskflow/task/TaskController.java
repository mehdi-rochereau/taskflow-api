package com.mehdi.taskflow.task;

import com.mehdi.taskflow.task.dto.TaskRequest;
import com.mehdi.taskflow.task.dto.TaskResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller handling task management operations within projects.
 *
 * <p>All endpoints require a valid JWT token passed as a
 * {@code Authorization: Bearer <token>} header.
 * Task mutations (create, update, delete) are restricted to the owner
 * of the associated project.</p>
 *
 * <p>Tasks are always accessed in the context of a parent project —
 * the {@code projectId} path variable is required on all endpoints.</p>
 *
 * <p>All responses are produced in {@code application/json} format.</p>
 *
 * @see TaskService
 */
@Tag(name = "Tâches", description = "Gestion des tâches par projet")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping(value = "/api/projects/{projectId}/tasks", produces = "application/json")
public class TaskController {

    private final TaskService taskService;

    /**
     * Constructs a new {@code TaskController} with its required dependency.
     *
     * @param taskService service handling task business logic
     */
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * Returns tasks belonging to a project, with optional filtering.
     *
     * <p>Filter priority: {@code status} takes precedence over {@code priority}.
     * If both are provided, only the status filter is applied.</p>
     *
     * @param projectId the project identifier
     * @param status    optional status filter — {@code TODO}, {@code IN_PROGRESS}, {@code DONE}
     * @param priority  optional priority filter — {@code LOW}, {@code MEDIUM}, {@code HIGH}
     * @return {@code 200 OK} with the list of matching tasks, empty array if none exist,
     *         or {@code 401 Unauthorized} if the JWT token is missing or invalid
     */
    @Operation(
            summary = "Lister les tâches d'un projet",
            description = "Retourne toutes les tâches d'un projet avec filtrage optionnel par statut ou priorité",
            parameters = {
                    @Parameter(name = "projectId", description = "ID du projet", required = true),
                    @Parameter(name = "status", description = "Filtrer par statut : TODO, IN_PROGRESS, DONE"),
                    @Parameter(name = "priority", description = "Filtrer par priorité : LOW, MEDIUM, HIGH")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Liste des tâches retournée avec succès",
                            content = @Content(schema = @Schema(implementation = TaskResponse.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide", content = @Content)
            }
    )
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getTasksByProject(
            @PathVariable Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority) {
        return ResponseEntity.ok(
                taskService.getTasksByProject(projectId, status, priority).stream()
                        .map(TaskResponse::new)
                        .toList()
        );
    }

    /**
     * Returns a task by its identifier.
     *
     * <p>Access is restricted to the owner of the associated project.</p>
     *
     * @param projectId the project identifier
     * @param id        the task identifier
     * @return {@code 200 OK} with the task details,
     *         {@code 401 Unauthorized} if the JWT token is missing or invalid,
     *         {@code 403 Forbidden} if the current user is not the project owner,
     *         or {@code 404 Not Found} if no task exists with the given id
     */
    @Operation(
            summary = "Récupérer une tâche par ID",
            description = "Retourne le détail d'une tâche si l'utilisateur est propriétaire du projet associé",
            parameters = {
                    @Parameter(name = "projectId", description = "ID du projet", required = true),
                    @Parameter(name = "id", description = "ID de la tâche", required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Tâche trouvée",
                            content = @Content(schema = @Schema(implementation = TaskResponse.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide", content = @Content),
                    @ApiResponse(responseCode = "403", description = "Accès refusé", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Tâche introuvable", content = @Content)
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(
            @PathVariable Long projectId,
            @PathVariable Long id) {
        return ResponseEntity.ok(new TaskResponse(taskService.getTaskById(id)));
    }

    /**
     * Creates a new task within a project.
     *
     * <p>Only the project owner can create tasks.
     * An optional {@code assigneeId} can be provided to assign the task
     * to any registered user.</p>
     *
     * @param projectId the identifier of the project to add the task to
     * @param request   the task data — title, status, priority, optional description,
     *                  due date and assignee
     * @return {@code 201 Created} with the created task,
     *         {@code 400 Bad Request} if validation fails,
     *         {@code 401 Unauthorized} if the JWT token is missing or invalid,
     *         {@code 403 Forbidden} if the current user is not the project owner,
     *         or {@code 404 Not Found} if the project or assignee does not exist
     */
    @Operation(
            summary = "Créer une tâche",
            description = "Crée une nouvelle tâche dans un projet dont l'utilisateur est propriétaire",
            parameters = @Parameter(name = "projectId", description = "ID du projet", required = true),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Tâche créée avec succès",
                            content = @Content(schema = @Schema(implementation = TaskResponse.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Données invalides", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide", content = @Content),
                    @ApiResponse(responseCode = "403", description = "Accès refusé — non propriétaire du projet", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Projet ou assignee introuvable", content = @Content)
            }
    )
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable Long projectId,
            @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new TaskResponse(taskService.createTask(projectId, request)));
    }

    /**
     * Updates an existing task.
     *
     * <p>Only the owner of the associated project can update tasks.</p>
     *
     * @param projectId the project identifier
     * @param id        the identifier of the task to update
     * @param request   the updated task data
     * @return {@code 200 OK} with the updated task,
     *         {@code 400 Bad Request} if validation fails,
     *         {@code 401 Unauthorized} if the JWT token is missing or invalid,
     *         {@code 403 Forbidden} if the current user is not the project owner,
     *         or {@code 404 Not Found} if the task or assignee does not exist
     */
    @Operation(
            summary = "Modifier une tâche",
            description = "Met à jour les informations d'une tâche dont l'utilisateur est propriétaire du projet",
            parameters = {
                    @Parameter(name = "projectId", description = "ID du projet", required = true),
                    @Parameter(name = "id", description = "ID de la tâche à modifier", required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Tâche modifiée avec succès",
                            content = @Content(schema = @Schema(implementation = TaskResponse.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Données invalides", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide", content = @Content),
                    @ApiResponse(responseCode = "403", description = "Accès refusé", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Tâche ou assignee introuvable", content = @Content)
            }
    )
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(new TaskResponse(taskService.updateTask(id, request)));
    }

    /**
     * Permanently deletes a task.
     *
     * <p>Only the owner of the associated project can delete tasks.</p>
     *
     * @param projectId the project identifier
     * @param id        the identifier of the task to delete
     * @return {@code 204 No Content} on success,
     *         {@code 401 Unauthorized} if the JWT token is missing or invalid,
     *         {@code 403 Forbidden} if the current user is not the project owner,
     *         or {@code 404 Not Found} if no task exists with the given id
     */
    @Operation(
            summary = "Supprimer une tâche",
            description = "Supprime définitivement une tâche dont l'utilisateur est propriétaire du projet",
            parameters = {
                    @Parameter(name = "projectId", description = "ID du projet", required = true),
                    @Parameter(name = "id", description = "ID de la tâche à supprimer", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "204", description = "Tâche supprimée avec succès", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide", content = @Content),
                    @ApiResponse(responseCode = "403", description = "Accès refusé", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Tâche introuvable", content = @Content)
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long projectId,
            @PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}