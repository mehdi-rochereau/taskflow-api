package com.mehdi.taskflow.task;

import com.mehdi.taskflow.exception.ResourceNotFoundException;
import com.mehdi.taskflow.project.Project;
import com.mehdi.taskflow.project.ProjectRepository;
import com.mehdi.taskflow.security.SecurityUtils;
import com.mehdi.taskflow.task.dto.TaskRequest;
import com.mehdi.taskflow.user.User;
import com.mehdi.taskflow.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    TaskRepository taskRepository;

    @Mock
    ProjectRepository projectRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    SecurityUtils securityUtils;

    @InjectMocks
    TaskService taskService;

    private User currentUser;
    private User otherUser;
    private Project project;
    private Task task;
    private TaskRequest taskRequest;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("mehdi");

        otherUser = new User();
        otherUser.setId(2L);

        project = new Project();
        project.setId(1L);
        project.setOwner(currentUser);

        task = new Task();
        task.setId(1L);
        task.setTitle("Ma tâche");
        task.setStatus(TaskStatus.TODO);
        task.setPriority(TaskPriority.MEDIUM);
        task.setProject(project);

        taskRequest = new TaskRequest();
        taskRequest.setTitle("Ma tâche");
        taskRequest.setStatus(TaskStatus.TODO);
        taskRequest.setPriority(TaskPriority.MEDIUM);
        taskRequest.setAssigneeId(2L);
    }

    private void givenAuthenticatedUser() {
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
    }

    @Test
    void getTasksByProject_shouldReturnAllTasks_whenNoFilter() {
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(task));
        List<Task> result = taskService.getTasksByProject(1L, null, null);
        assertEquals(1, result.size());
        assertEquals("Ma tâche", result.getFirst().getTitle());
        verify(taskRepository).findByProjectId(1L);
    }

    @Test
    void getTasksByProject_shouldFilterByStatus() {
        when(taskRepository.findByProjectIdAndStatus(1L, TaskStatus.TODO)).thenReturn(List.of(task));
        List<Task> result = taskService.getTasksByProject(1L, TaskStatus.TODO, null);
        assertEquals(1, result.size());
        verify(taskRepository).findByProjectIdAndStatus(1L, TaskStatus.TODO);
    }

    @Test
    void getTasksByProject_shouldFilterByPriority() {
        when(taskRepository.findByProjectIdAndPriority(1L, TaskPriority.MEDIUM)).thenReturn(List.of(task));
        List<Task> tasks = taskService.getTasksByProject(1L, null, TaskPriority.MEDIUM);
        assertEquals(1, tasks.size());
        verify(taskRepository).findByProjectIdAndPriority(1L, TaskPriority.MEDIUM);
    }

    @Test
    void getTaskById_shouldReturnTask_whenOwner() {
        // GIVEN
        givenAuthenticatedUser();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.existsByIdAndProjectOwnerId(1L, 1L)).thenReturn(true);

        // WHEN
        Task result = taskService.getTaskById(1L);

        // THEN
        assertNotNull(result);
        assertEquals("Ma tâche", result.getTitle());
    }

    @Test
    void getTaskById_shouldThrow_whenTaskNotFound() {
        // GIVEN
        givenAuthenticatedUser();
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> taskService.getTaskById(999L));
    }

    @Test
    void getTaskById_shouldThrow_whenNotOwner() {
        // GIVEN
        givenAuthenticatedUser();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.existsByIdAndProjectOwnerId(1L, 1L)).thenReturn(false);
        assertThrows(AccessDeniedException.class,
                () -> taskService.getTaskById(1L));
    }

    @Test
    void createTask_shouldCreateAndReturnTask_whenOwner() {
        // GIVEN
        givenAuthenticatedUser();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // WHEN
        Task result = taskService.createTask(1L, taskRequest);

        // THEN
        assertNotNull(result);
        assertEquals("Ma tâche", result.getTitle());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void createTask_shouldCreateAndReturnTask_whenOwnerAndNoAssignee() {
        // GIVEN
        givenAuthenticatedUser();
        taskRequest.setAssigneeId(null);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // WHEN
        Task result = taskService.createTask(1L, taskRequest);

        // THEN
        assertNotNull(result);
        verify(userRepository, never()).findById(any());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void createTask_shouldThrow_whenProjectNotFound() {
        // GIVEN
        givenAuthenticatedUser();
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> taskService.createTask(999L, taskRequest));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void createTask_shouldThrow_whenNotProjectOwner() {
        // GIVEN
        givenAuthenticatedUser();
        User otherOwner = new User();
        otherOwner.setId(2L);
        project.setOwner(otherOwner);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        // WHEN & THEN
        assertThrows(AccessDeniedException.class,
                () -> taskService.createTask(1L, taskRequest));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void createTask_shouldThrow_whenUserAssignedNotFound() {
        // GIVEN
        givenAuthenticatedUser();
        taskRequest.setAssigneeId(99L);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(ResourceNotFoundException.class,
                () -> taskService.createTask(1L, taskRequest));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void updateTask_shouldUpdateAndReturnTask_whenOwner() {
        // GIVEN
        givenAuthenticatedUser();
        when(taskRepository.existsByIdAndProjectOwnerId(1L, 1L)).thenReturn(true);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // WHEN
        Task result = taskService.updateTask(1L, taskRequest);

        // THEN
        assertNotNull(result);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void updateTask_shouldUpdateAndReturnTask_whenOwnerAndNoAssignee() {
        // GIVEN
        givenAuthenticatedUser();
        taskRequest.setAssigneeId(null);
        when(taskRepository.existsByIdAndProjectOwnerId(1L, 1L)).thenReturn(true);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // WHEN
        Task result = taskService.updateTask(1L, taskRequest);

        // THEN
        assertNotNull(result);
        verify(userRepository, never()).findById(any());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void updateTask_shouldThrow_whenNotOwner() {
        // GIVEN
        givenAuthenticatedUser();
        when(taskRepository.existsByIdAndProjectOwnerId(1L, 1L)).thenReturn(false);
        assertThrows(AccessDeniedException.class,
                () -> taskService.updateTask(1L, taskRequest));
        verify(taskRepository, never()).findById(any());
    }

    @Test
    void updateTask_shouldThrow_whenTaskNotFound() {
        // GIVEN
        givenAuthenticatedUser();
        when(taskRepository.existsByIdAndProjectOwnerId(1L, 1L)).thenReturn(true);
        when(taskRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> taskService.updateTask(1L, taskRequest));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void updateTask_shouldThrow_whenUserAssignedNotFound() {
        // GIVEN
        givenAuthenticatedUser();
        taskRequest.setAssigneeId(99L);
        when(taskRepository.existsByIdAndProjectOwnerId(1L, 1L)).thenReturn(true);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(ResourceNotFoundException.class,
                () -> taskService.updateTask(1L, taskRequest));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void deleteTask_shouldThrow_whenNotOwner() {
        // GIVEN
        givenAuthenticatedUser();
        when(taskRepository.existsByIdAndProjectOwnerId(1L, 1L)).thenReturn(false);
        assertThrows(AccessDeniedException.class,
                () -> taskService.deleteTask(1L));
        verify(taskRepository, never()).findById(any());
        verify(taskRepository, never()).delete(any());
    }

    @Test
    void deleteTask_shouldThrow_whenTaskNotFound() {
        // GIVEN
        givenAuthenticatedUser();
        when(taskRepository.existsByIdAndProjectOwnerId(1L, 1L)).thenReturn(true);
        when(taskRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> taskService.deleteTask(1L));
        verify(taskRepository, never()).delete(any());
    }

    @Test
    void deleteTask_shouldDelete_whenOwner() {
        // GIVEN
        givenAuthenticatedUser();
        when(taskRepository.existsByIdAndProjectOwnerId(1L, 1L)).thenReturn(true);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        taskService.deleteTask(1L);
        verify(taskRepository, times(1)).delete(task);
    }
}