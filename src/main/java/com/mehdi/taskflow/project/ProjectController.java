package com.mehdi.taskflow.project;

import com.mehdi.taskflow.project.dto.ProjectRequest;
import com.mehdi.taskflow.project.dto.ProjectResponse;
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

@Tag(name = "Projets", description = "Gestion des projets de l'utilisateur connecté")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Operation(
            summary = "Lister mes projets",
            description = "Retourne tous les projets appartenant à l'utilisateur connecté",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Liste des projets retournée avec succès",
                            content = @Content(schema = @Schema(implementation = ProjectResponse.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide", content = @Content)
            }
    )
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getMyProjects() {
        return ResponseEntity.ok(
                projectService.getMyProjects().stream()
                        .map(ProjectResponse::new)
                        .toList()
        );
    }

    @Operation(
            summary = "Récupérer un projet par ID",
            description = "Retourne le détail d'un projet si l'utilisateur en est le propriétaire",
            parameters = @Parameter(name = "id", description = "ID du projet", required = true),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Projet trouvé",
                            content = @Content(schema = @Schema(implementation = ProjectResponse.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide", content = @Content),
                    @ApiResponse(responseCode = "403", description = "Accès refusé — projet appartenant à un autre utilisateur", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Projet introuvable", content = @Content)
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long id) {
        return ResponseEntity.ok(new ProjectResponse(projectService.getProjectById(id)));
    }

    @Operation(
            summary = "Créer un projet",
            description = "Crée un nouveau projet associé à l'utilisateur connecté",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Projet créé avec succès",
                            content = @Content(schema = @Schema(implementation = ProjectResponse.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Données invalides — nom manquant ou trop long", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide", content = @Content)
            }
    )
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ProjectResponse(projectService.createProject(request)));
    }


    @Operation(
            summary = "Modifier un projet",
            description = "Met à jour le nom et la description d'un projet dont l'utilisateur est propriétaire",
            parameters = @Parameter(name = "id", description = "ID du projet à modifier", required = true),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Projet modifié avec succès",
                            content = @Content(schema = @Schema(implementation = ProjectResponse.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Données invalides", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide", content = @Content),
                    @ApiResponse(responseCode = "403", description = "Accès refusé — projet appartenant à un autre utilisateur", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Projet introuvable", content = @Content)
            }
    )
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable Long id,
                                                         @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.ok(new ProjectResponse(projectService.updateProject(id, request)));
    }

    @Operation(
            summary = "Supprimer un projet",
            description = "Supprime définitivement un projet dont l'utilisateur est propriétaire",
            parameters = @Parameter(name = "id", description = "ID du projet à supprimer", required = true),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Projet supprimé avec succès", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide", content = @Content),
                    @ApiResponse(responseCode = "403", description = "Accès refusé — projet appartenant à un autre utilisateur", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Projet introuvable", content = @Content)
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}