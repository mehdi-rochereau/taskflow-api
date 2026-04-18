package com.mehdi.taskflow.task;

import com.mehdi.taskflow.task.dto.TaskRequest;
import com.mehdi.taskflow.task.dto.TaskResponse;
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
@RestController
@RequestMapping(value = "/api/projects/{projectId}/tasks", produces = "application/json")
public class TaskController implements TaskControllerApi {

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
     * or {@code 401 Unauthorized} if the JWT token is missing or invalid
     */
    @Override
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
     * {@code 401 Unauthorized} if the JWT token is missing or invalid,
     * {@code 403 Forbidden} if the current user is not the project owner,
     * or {@code 404 Not Found} if no task exists with the given id
     */
    @Override
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
     * {@code 400 Bad Request} if validation fails,
     * {@code 401 Unauthorized} if the JWT token is missing or invalid,
     * {@code 403 Forbidden} if the current user is not the project owner,
     * or {@code 404 Not Found} if the project or assignee does not exist
     */
    @Override
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
     * {@code 400 Bad Request} if validation fails,
     * {@code 401 Unauthorized} if the JWT token is missing or invalid,
     * {@code 403 Forbidden} if the current user is not the project owner,
     * or {@code 404 Not Found} if the task or assignee does not exist
     */
    @Override
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
     * {@code 401 Unauthorized} if the JWT token is missing or invalid,
     * {@code 403 Forbidden} if the current user is not the project owner,
     * or {@code 404 Not Found} if no task exists with the given id
     */
    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long projectId,
            @PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}