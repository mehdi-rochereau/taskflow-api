package com.mehdi.taskflow.task;

import com.mehdi.taskflow.exception.ResourceNotFoundException;
import com.mehdi.taskflow.project.Project;
import com.mehdi.taskflow.project.ProjectRepository;
import com.mehdi.taskflow.security.SecurityUtils;
import com.mehdi.taskflow.task.dto.TaskRequest;
import com.mehdi.taskflow.user.User;
import com.mehdi.taskflow.user.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service handling task management operations within projects.
 *
 * <p>All operations are restricted to authenticated users.
 * Task mutations (create, update, delete) require the current user
 * to be the owner of the associated project.</p>
 *
 * <p>Tasks can optionally be assigned to any registered user via {@code assigneeId}.
 * If no assignee is specified, the task remains unassigned.</p>
 *
 * <p>The authenticated user is resolved via {@link SecurityUtils}.</p>
 *
 * @see SecurityUtils
 * @see TaskRepository
 * @see ProjectRepository
 */
@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    /**
     * Constructs a new {@code TaskService} with its required dependencies.
     *
     * @param taskRepository    repository for task persistence
     * @param projectRepository repository for project lookups
     * @param userRepository    repository for assignee lookups
     * @param securityUtils     utility for resolving the currently authenticated user
     */
    public TaskService(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       UserRepository userRepository,
                       SecurityUtils securityUtils) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * Returns tasks belonging to a project, with optional filtering.
     *
     * <p>Filter priority: {@code status} takes precedence over {@code priority}.
     * If both are provided, only the status filter is applied.</p>
     *
     * @param projectId the project identifier
     * @param status    optional status filter — returns only tasks with this status
     * @param priority  optional priority filter — returns only tasks with this priority
     * @return list of matching tasks, empty if none found
     */
    @PreAuthorize("isAuthenticated()")
    public List<Task> getTasksByProject(Long projectId, TaskStatus status, TaskPriority priority) {
        if (status != null) {
            return taskRepository.findByProjectIdAndStatus(projectId, status);
        }
        if (priority != null) {
            return taskRepository.findByProjectIdAndPriority(projectId, priority);
        }
        return taskRepository.findByProjectId(projectId);
    }

    /**
     * Returns a task by its identifier.
     *
     * <p>Access is restricted to the owner of the project the task belongs to.</p>
     *
     * @param id the task identifier
     * @return the matching task
     * @throws ResourceNotFoundException if no task exists with the given id
     * @throws AccessDeniedException     if the current user is not the project owner
     */
    @PreAuthorize("isAuthenticated()")
    public Task getTaskById(Long id) {
        User currentUser = securityUtils.getCurrentUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche introuvable"));
        if (!taskRepository.existsByIdAndProjectOwnerId(id, currentUser.getId())) {
            throw new AccessDeniedException("Accès refusé");
        }
        return task;
    }

    /**
     * Creates a new task within a project.
     *
     * <p>Only the project owner can create tasks.
     * If an {@code assigneeId} is provided, the assignee must exist in the database.</p>
     *
     * @param projectId the identifier of the project to add the task to
     * @param request   data for the task to create
     * @return the persisted task with its generated identifier
     * @throws ResourceNotFoundException if the project or the assignee does not exist
     * @throws AccessDeniedException     if the current user is not the project owner
     */
    @PreAuthorize("isAuthenticated()")
    public Task createTask(Long projectId, TaskRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable"));
        if (!project.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Accès refusé");
        }
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        task.setProject(project);
        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee introuvable"));
            task.setAssignee(assignee);
        }
        return taskRepository.save(task);
    }

    /**
     * Updates an existing task.
     *
     * <p>Ownership is verified before loading the task —
     * if the current user is not the project owner, the task is never fetched.</p>
     *
     * @param id      the identifier of the task to update
     * @param request updated task data
     * @return the updated task
     * @throws AccessDeniedException     if the current user is not the project owner
     * @throws ResourceNotFoundException if the task or the assignee does not exist
     */
    @PreAuthorize("isAuthenticated()")
    public Task updateTask(Long id, TaskRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        if (!taskRepository.existsByIdAndProjectOwnerId(id, currentUser.getId())) {
            throw new AccessDeniedException("Accès refusé");
        }
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche introuvable"));
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee introuvable"));
            task.setAssignee(assignee);
        }
        return taskRepository.save(task);
    }

    /**
     * Permanently deletes a task.
     *
     * <p>Ownership is verified before loading the task —
     * if the current user is not the project owner, the task is never fetched.</p>
     *
     * @param id the identifier of the task to delete
     * @throws AccessDeniedException     if the current user is not the project owner
     * @throws ResourceNotFoundException if no task exists with the given id
     */
    @PreAuthorize("isAuthenticated()")
    public void deleteTask(Long id) {
        User currentUser = securityUtils.getCurrentUser();
        if (!taskRepository.existsByIdAndProjectOwnerId(id, currentUser.getId())) {
            throw new AccessDeniedException("Accès refusé");
        }
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche introuvable"));
        taskRepository.delete(task);
    }
}