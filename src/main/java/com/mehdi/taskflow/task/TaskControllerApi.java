package com.mehdi.taskflow.task;

import com.mehdi.taskflow.task.dto.TaskRequest;
import com.mehdi.taskflow.task.dto.TaskResponse;
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
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(
        name = "Tasks",
        description = "Task management endpoints within projects. All operations require project ownership."
)
@SecurityRequirement(name = "bearerAuth")
public interface TaskControllerApi {

    @Operation(
            summary = "List tasks of a project",
            description = """
                    Returns all tasks of a project with optional filtering by status or priority.
                    
                    **Filter priority:** if both `status` and `priority` are provided, only `status` is applied.
                    """,
            parameters = {
                    @Parameter(name = "projectId", description = "Project identifier", required = true),
                    @Parameter(name = "status", description = "Filter by status. If provided, priority filter is ignored."),
                    @Parameter(name = "priority", description = "Filter by priority. Ignored if status is provided."),
                    @Parameter(ref = "#/components/parameters/Accept-Language")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Tasks successfully retrieved",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    [
                                                      {
                                                        "id": 1,
                                                        "title": "Implement JWT authentication",
                                                        "description": "Add Spring Security with JWT filter",
                                                        "status": "TODO",
                                                        "priority": "HIGH",
                                                        "dueDate": "2026-12-31",
                                                        "projectId": 1,
                                                        "assigneeUsername": "mehdi",
                                                        "createdAt": "2026-04-18T10:00:00"
                                                      }
                                                    ]
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid status or priority value",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 400,
                                                      "message": "Parameter 'status' must be of type TaskStatus"
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
                    )
            }
    )
    ResponseEntity<List<TaskResponse>> getTasksByProject(
            @PathVariable Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority);

    @Operation(
            summary = "Get a task by ID",
            description = "Returns a task by its identifier. Access is restricted to the owner of the associated project.",
            parameters = {
                    @Parameter(name = "projectId", description = "Project identifier", required = true),
                    @Parameter(name = "id", description = "Task identifier", required = true),
                    @Parameter(ref = "#/components/parameters/Accept-Language")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Task successfully retrieved",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TaskResponse.class),
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "id": 1,
                                                      "title": "Implement JWT authentication",
                                                      "description": "Add Spring Security with JWT filter",
                                                      "status": "TODO",
                                                      "priority": "HIGH",
                                                      "dueDate": "2026-12-31",
                                                      "projectId": 1,
                                                      "assigneeUsername": "mehdi",
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
                            description = "Access denied — current user is not the project owner",
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
                            description = "Task not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 404,
                                                      "message": "Task not found"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<TaskResponse> getTaskById(
            @PathVariable Long projectId,
            @PathVariable Long id);

    @Operation(
            summary = "Create a task",
            description = """
                    Creates a new task in a project. Only the project owner can create tasks.
                    
                    An optional `assigneeId` can be provided to assign the task to any registered user.
                    """,
            parameters = {
                    @Parameter(name = "projectId", description = "Project identifier", required = true),
                    @Parameter(ref = "#/components/parameters/Accept-Language")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Task successfully created",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TaskResponse.class),
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "id": 1,
                                                      "title": "Implement JWT authentication",
                                                      "description": "Add Spring Security with JWT filter",
                                                      "status": "TODO",
                                                      "priority": "HIGH",
                                                      "dueDate": "2026-12-31",
                                                      "projectId": 1,
                                                      "assigneeUsername": "mehdi",
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
                                                        "title": ["Task title is required"],
                                                        "status": ["Task status is required"],
                                                        "priority": ["Task priority is required"]
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
                            description = "Access denied — current user is not the project owner",
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
                            description = "Project or assignee not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    name = "Project not found",
                                                    value = """
                                                            {
                                                              "timestamp": "2026-04-18T10:00:00",
                                                              "status": 404,
                                                              "message": "Project not found"
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "Assignee not found",
                                                    value = """
                                                            {
                                                              "timestamp": "2026-04-18T10:00:00",
                                                              "status": 404,
                                                              "message": "Assignee not found"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    ResponseEntity<TaskResponse> createTask(
            @PathVariable Long projectId,
            @Valid @RequestBody TaskRequest request);


    @Operation(
            summary = "Update a task",
            description = "Updates a task. Only the owner of the associated project can update tasks.",
            parameters = {
                    @Parameter(name = "projectId", description = "Project identifier", required = true),
                    @Parameter(name = "id", description = "Task identifier", required = true),
                    @Parameter(ref = "#/components/parameters/Accept-Language")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Task successfully updated",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TaskResponse.class),
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "id": 1,
                                                      "title": "Implement JWT authentication",
                                                      "description": "Updated description",
                                                      "status": "IN_PROGRESS",
                                                      "priority": "HIGH",
                                                      "dueDate": "2026-12-31",
                                                      "projectId": 1,
                                                      "assigneeUsername": "mehdi",
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
                                                        "title": ["Task title is required"],
                                                        "status": ["Task status is required"],
                                                        "priority": ["Task priority is required"]
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
                            description = "Access denied — current user is not the project owner",
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
                            description = "Task or assignee not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    name = "Task not found",
                                                    value = """
                                                            {
                                                              "timestamp": "2026-04-18T10:00:00",
                                                              "status": 404,
                                                              "message": "Task not found"
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "Assignee not found",
                                                    value = """
                                                            {
                                                              "timestamp": "2026-04-18T10:00:00",
                                                              "status": 404,
                                                              "message": "Assignee not found"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request);

    @Operation(
            summary = "Delete a task",
            description = "Permanently deletes a task. Only the owner of the associated project can delete tasks.",
            parameters = {
                    @Parameter(name = "projectId", description = "Project identifier", required = true),
                    @Parameter(name = "id", description = "Task identifier", required = true),
                    @Parameter(ref = "#/components/parameters/Accept-Language")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Task successfully deleted",
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
                            description = "Access denied — current user is not the project owner",
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
                            description = "Task not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 404,
                                                      "message": "Task not found"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<Void> deleteTask(
            @PathVariable Long projectId,
            @PathVariable Long id);
}
