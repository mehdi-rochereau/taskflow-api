package com.mehdi.taskflow.project;

import com.mehdi.taskflow.project.dto.ProjectRequest;
import com.mehdi.taskflow.project.dto.ProjectResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(
        name = "Projects",
        description = "Project management endpoints. All operations are scoped to the authenticated user."
)
@SecurityRequirement(name = "bearerAuth")
public interface ProjectControllerApi {

    @Operation(
            summary = "List my projects",
            description = "Returns all projects owned by the authenticated user. Returns an empty array if none exist.",
            parameters = {
                    @Parameter(ref = "#/components/parameters/Accept-Language")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Projects successfully retrieved",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    [
                                                      {
                                                        "id": 1,
                                                        "name": "TaskFlow Backend",
                                                        "description": "REST API built with Spring Boot 3.5",
                                                        "ownerUsername": "mehdi",
                                                        "createdAt": "2026-04-18T10:00:00"
                                                      }
                                                    ]
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Missing or invalid JWT token",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 401,
                                                      "message": "Authentication required"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<List<ProjectResponse>> getMyProjects();

    @Operation(
            summary = "Get a project by ID",
            description = "Returns a project by its identifier. Access is restricted to the project owner.",
            parameters = {
                    @Parameter(name = "id", description = "Project identifier", required = true),
                    @Parameter(ref = "#/components/parameters/Accept-Language")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Project successfully retrieved",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ProjectResponse.class),
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "id": 1,
                                                      "name": "TaskFlow Backend",
                                                      "description": "REST API built with Spring Boot 3.5",
                                                      "ownerUsername": "mehdi",
                                                      "createdAt": "2026-04-18T10:00:00"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Missing or invalid JWT token",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 401,
                                                      "message": "Authentication required"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied — project belongs to another user",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 403,
                                                      "message": "Access denied"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Project not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 404,
                                                      "message": "Project not found"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long id);

    @Operation(
            summary = "Create a project",
            description = "Creates a new project associated with the authenticated user.",
            parameters = {
                    @Parameter(ref = "#/components/parameters/Accept-Language")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Project successfully created",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ProjectResponse.class),
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "id": 1,
                                                      "name": "TaskFlow Backend",
                                                      "description": "REST API built with Spring Boot 3.5",
                                                      "ownerUsername": "mehdi",
                                                      "createdAt": "2026-04-18T10:00:00"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Validation failed",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    name = "Name required",
                                                    value = """
                                                            {
                                                              "timestamp": "2026-04-18T10:00:00",
                                                              "status": 400,
                                                              "errors": {
                                                                "name": ["Project name is required"]
                                                              }
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "Name too long",
                                                    value = """
                                                            {
                                                              "timestamp": "2026-04-18T10:00:00",
                                                              "status": 400,
                                                              "errors": {
                                                                "name": ["Project name must not exceed 100 characters"]
                                                              }
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Missing or invalid JWT token",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 401,
                                                      "message": "Authentication required"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request);

    @Operation(
            summary = "Update a project",
            description = "Updates the name and description of a project. Only the project owner can perform this operation.",
            parameters = {
                    @Parameter(name = "id", description = "Project identifier", required = true),
                    @Parameter(ref = "#/components/parameters/Accept-Language")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Project successfully updated",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ProjectResponse.class),
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "id": 1,
                                                      "name": "TaskFlow Backend v2",
                                                      "description": "Updated description",
                                                      "ownerUsername": "mehdi",
                                                      "createdAt": "2026-04-18T10:00:00"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Validation failed",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 400,
                                                      "errors": {
                                                        "name": ["Project name is required"]
                                                      }
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Missing or invalid JWT token",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 401,
                                                      "message": "Authentication required"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied — project belongs to another user",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 403,
                                                      "message": "Access denied"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Project not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 404,
                                                      "message": "Project not found"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<ProjectResponse> updateProject(@PathVariable Long id,
                                                  @Valid @RequestBody ProjectRequest request);

    @Operation(
            summary = "Delete a project",
            description = "Permanently deletes a project. Only the project owner can perform this operation.",
            parameters = {
                    @Parameter(name = "id", description = "Project identifier", required = true),
                    @Parameter(ref = "#/components/parameters/Accept-Language")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Project successfully deleted",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Missing or invalid JWT token",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 401,
                                                      "message": "Authentication required"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied — project belongs to another user",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 403,
                                                      "message": "Access denied"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Project not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 404,
                                                      "message": "Project not found"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<Void> deleteProject(@PathVariable Long id);
}
