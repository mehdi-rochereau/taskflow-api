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

@Tag(name = "Tâches", description = "Gestion des tâches par projet")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

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