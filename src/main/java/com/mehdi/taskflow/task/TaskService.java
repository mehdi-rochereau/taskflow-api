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

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    public TaskService(TaskRepository taskRepository, ProjectRepository projectRepository, UserRepository userRepository, SecurityUtils securityUtils) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
    }

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