package com.mehdi.taskflow.project;

import com.mehdi.taskflow.project.dto.ProjectRequest;
import com.mehdi.taskflow.project.dto.ProjectResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * OpenAPI documentation interface for {@link ProjectController}.
 *
 * <p>Declares all Swagger/OpenAPI annotations for project endpoints, keeping {@link
 * ProjectController} clean and focused on business logic.
 *
 * <p>All endpoints require the {@code jwt} HttpOnly cookie, which the client receives on login and
 * sends back automatically.
 *
 * @see ProjectController
 */
@Tag(
        name = "Projects",
        description =
                "Project management endpoints. All operations are scoped to the authenticated user.")
public interface ProjectControllerApi {

    /**
     * Lists the projects owned by the authenticated user.
     *
     * <p>Implemented by {@link ProjectController}; this interface carries the OpenAPI annotations
     * only. Ownership filtering happens in the service layer, the caller cannot widen the scope.
     *
     * @return {@code 200 OK} with the caller's projects, an empty array if none
     */
    @Operation(
            summary = "List my projects",
            description =
                    "Returns all projects owned by the authenticated user. Returns an empty array if none exist.",
            parameters = {@Parameter(ref = "#/components/parameters/Accept-Language")},
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Projects successfully retrieved",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                                    [
                                                      {
                                                        "id": 1,
                                                        "name": "TaskFlow Backend",
                                                        "description": "REST API built with Spring Boot 3.5",
                                                        "ownerUsername": "mehdi",
                                                        "createdAt": "2026-04-18T10:00:00"
                                                      }
                                                    ]
                                                    """))),
                @ApiResponse(
                        responseCode = "401",
                        description = "Missing or invalid JWT token",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 401,
                                                      "message": "Authentication required"
                                                    }
                                                    """)))
            })
    ResponseEntity<List<ProjectResponse>> getMyProjects();

    /**
     * Returns a single project by identifier.
     *
     * <p>Implemented by {@link ProjectController}; this interface carries the OpenAPI annotations
     * only. A project owned by another user answers {@code 404} rather than {@code 403}, so the
     * response does not reveal whether the identifier exists.
     *
     * @param id the project identifier
     * @return {@code 200 OK} with the project, or {@code 404} if it does not exist or is owned by
     *     someone else
     */
    @Operation(
            summary = "Get a project by ID",
            description =
                    "Returns a project by its identifier. Access is restricted to the project owner.",
            parameters = {
                @Parameter(name = "id", description = "Project identifier", required = true),
                @Parameter(ref = "#/components/parameters/Accept-Language")
            },
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Project successfully retrieved",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ProjectResponse.class),
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                                    {
                                                      "id": 1,
                                                      "name": "TaskFlow Backend",
                                                      "description": "REST API built with Spring Boot 3.5",
                                                      "ownerUsername": "mehdi",
                                                      "createdAt": "2026-04-18T10:00:00"
                                                    }
                                                    """))),
                @ApiResponse(
                        responseCode = "401",
                        description = "Missing or invalid JWT token",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 401,
                                                      "message": "Authentication required"
                                                    }
                                                    """))),
                @ApiResponse(
                        responseCode = "403",
                        description = "Access denied — project belongs to another user",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 403,
                                                      "message": "Access denied"
                                                    }
                                                    """))),
                @ApiResponse(
                        responseCode = "404",
                        description = "Project not found",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 404,
                                                      "message": "Project not found"
                                                    }
                                                    """)))
            })
    ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long id);

    /**
     * Creates a project owned by the authenticated user.
     *
     * <p>Implemented by {@link ProjectController}; this interface carries the OpenAPI annotations
     * only. The owner is taken from the security context, never from the request body.
     *
     * @param request the project data, name and description
     * @return {@code 201 Created} with the persisted project and its generated identifier
     */
    @Operation(
            summary = "Create a project",
            description = "Creates a new project associated with the authenticated user.",
            parameters = {@Parameter(ref = "#/components/parameters/Accept-Language")},
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Project successfully created",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ProjectResponse.class),
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                                    {
                                                      "id": 1,
                                                      "name": "TaskFlow Backend",
                                                      "description": "REST API built with Spring Boot 3.5",
                                                      "ownerUsername": "mehdi",
                                                      "createdAt": "2026-04-18T10:00:00"
                                                    }
                                                    """))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Validation failed",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    name = "Name required",
                                                    value =
                                                            """
                                                            {
                                                              "timestamp": "2026-04-18T10:00:00",
                                                              "status": 400,
                                                              "errors": {
                                                                "name": ["Project name is required"]
                                                              }
                                                            }
                                                            """),
                                            @ExampleObject(
                                                    name = "Name too long",
                                                    value =
                                                            """
                                                            {
                                                              "timestamp": "2026-04-18T10:00:00",
                                                              "status": 400,
                                                              "errors": {
                                                                "name": ["Project name must not exceed 100 characters"]
                                                              }
                                                            }
                                                            """)
                                        })),
                @ApiResponse(
                        responseCode = "401",
                        description = "Missing or invalid JWT token",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 401,
                                                      "message": "Authentication required"
                                                    }
                                                    """)))
            })
    ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request);

    /**
     * Updates a project owned by the authenticated user.
     *
     * <p>Implemented by {@link ProjectController}; this interface carries the OpenAPI annotations
     * only. Ownership is verified in the service layer before any write.
     *
     * @param id the identifier of the project to update
     * @param request the new project data
     * @return {@code 200 OK} with the updated project, or {@code 404} if it does not exist or is
     *     owned by someone else
     */
    @Operation(
            summary = "Update a project",
            description =
                    "Updates the name and description of a project. Only the project owner can perform this operation.",
            parameters = {
                @Parameter(name = "id", description = "Project identifier", required = true),
                @Parameter(ref = "#/components/parameters/Accept-Language")
            },
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Project successfully updated",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ProjectResponse.class),
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                                    {
                                                      "id": 1,
                                                      "name": "TaskFlow Backend v2",
                                                      "description": "Updated description",
                                                      "ownerUsername": "mehdi",
                                                      "createdAt": "2026-04-18T10:00:00"
                                                    }
                                                    """))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Validation failed",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 400,
                                                      "errors": {
                                                        "name": ["Project name is required"]
                                                      }
                                                    }
                                                    """))),
                @ApiResponse(
                        responseCode = "401",
                        description = "Missing or invalid JWT token",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 401,
                                                      "message": "Authentication required"
                                                    }
                                                    """))),
                @ApiResponse(
                        responseCode = "403",
                        description = "Access denied — project belongs to another user",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 403,
                                                      "message": "Access denied"
                                                    }
                                                    """))),
                @ApiResponse(
                        responseCode = "404",
                        description = "Project not found",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 404,
                                                      "message": "Project not found"
                                                    }
                                                    """)))
            })
    ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id, @Valid @RequestBody ProjectRequest request);

    /**
     * Permanently deletes a project.
     *
     * <p>Implemented by {@link ProjectController}; this interface carries the OpenAPI annotations
     * only. The project's tasks are removed by the database through {@code fk_tasks_project ON
     * DELETE CASCADE}, not by application code.
     *
     * @param id the identifier of the project to delete
     * @return {@code 204 No Content}, or {@code 404} if the project does not exist or is owned by
     *     someone else
     */
    @Operation(
            summary = "Delete a project",
            description =
                    "Permanently deletes a project. Only the project owner can perform this operation.",
            parameters = {
                @Parameter(name = "id", description = "Project identifier", required = true),
                @Parameter(ref = "#/components/parameters/Accept-Language")
            },
            responses = {
                @ApiResponse(
                        responseCode = "204",
                        description = "Project successfully deleted",
                        content = @Content),
                @ApiResponse(
                        responseCode = "401",
                        description = "Missing or invalid JWT token",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 401,
                                                      "message": "Authentication required"
                                                    }
                                                    """))),
                @ApiResponse(
                        responseCode = "403",
                        description = "Access denied — project belongs to another user",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 403,
                                                      "message": "Access denied"
                                                    }
                                                    """))),
                @ApiResponse(
                        responseCode = "404",
                        description = "Project not found",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 404,
                                                      "message": "Project not found"
                                                    }
                                                    """)))
            })
    ResponseEntity<Void> deleteProject(@PathVariable Long id);
}
