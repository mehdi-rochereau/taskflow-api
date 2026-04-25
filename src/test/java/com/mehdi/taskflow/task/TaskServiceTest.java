package com.mehdi.taskflow.task;

import com.mehdi.taskflow.config.AuditService;
import com.mehdi.taskflow.config.MessageService;
import com.mehdi.taskflow.config.SanitizationService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private MessageService messageService;

    @Mock
    private AuditService auditService;

    @Mock
    private SanitizationService sanitizationService;

    @InjectMocks
    private TaskService taskService;

    private User currentUser;
    private User otherUser;
    private Project project;
    private Task task;
    private TaskRequest createRequest;
    private TaskRequest updateRequest;

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
        task.setTitle("Existing task");
        task.setDescription("Existing description");
        task.setStatus(TaskStatus.TODO);
        task.setPriority(TaskPriority.MEDIUM);
        task.setDueDate(LocalDate.of(2026, 12, 31));
        task.setProject(project);
        task.setAssignee(otherUser);

        createRequest = new TaskRequest();
        createRequest.setTitle("New task");
        createRequest.setDescription("New description");
        createRequest.setStatus(TaskStatus.TODO);
        createRequest.setPriority(TaskPriority.MEDIUM);
        createRequest.setDueDate(LocalDate.of(2026, 12, 31));
        createRequest.setAssigneeId(2L);

        updateRequest = new TaskRequest();
        updateRequest.setTitle("Updated task");
        updateRequest.setDescription("Updated description");
        updateRequest.setStatus(TaskStatus.TODO);
        updateRequest.setPriority(TaskPriority.MEDIUM);
        updateRequest.setDueDate(LocalDate.of(2026, 12, 31));
        updateRequest.setAssigneeId(2L);
    }

    private void givenAuthenticatedUser() {
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
    }

    @Test
    void getTasksByProject_shouldReturnAllTasks_whenNoFilter() {
        // GIVEN
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(task));

        // WHEN
        List<Task> result = taskService.getTasksByProject(1L, null, null);

        // THEN
        assertEquals(1, result.size());
        assertEquals("Existing task", result.getFirst().getTitle());
        assertEquals("Existing description", result.getFirst().getDescription());
        assertEquals(TaskStatus.TODO, result.getFirst().getStatus());
        assertEquals(TaskPriority.MEDIUM, result.getFirst().getPriority());
        assertEquals(LocalDate.of(2026, 12, 31), result.getFirst().getDueDate());
        assertEquals(otherUser, result.getFirst().getAssignee());
        assertEquals(project, result.getFirst().getProject());
        assertEquals(currentUser, result.getFirst().getProject().getOwner());
        verify(taskRepository, never()).findByProjectIdAndStatus(eq(1L), any());
        verify(taskRepository, never()).findByProjectIdAndPriority(eq(1L), any());
        verify(taskRepository).findByProjectId(1L);
        verify(securityUtils, never()).getCurrentUser();
    }

    @Test
    void getTasksByProject_shouldReturnEmptyList_whenNoTasks() {
        // GIVEN
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of());

        // WHEN
        List<Task> result = taskService.getTasksByProject(1L, null, null);

        // THEN
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(taskRepository, never()).findByProjectIdAndStatus(eq(1L), any());
        verify(taskRepository, never()).findByProjectIdAndPriority(eq(1L), any());
        verify(taskRepository).findByProjectId(1L);
        verify(securityUtils, never()).getCurrentUser();
    }

    @Test
    void getTasksByProject_shouldFilterByStatus() {
        // GIVEN
        when(taskRepository.findByProjectIdAndStatus(1L, TaskStatus.TODO)).thenReturn(List.of(task));

        // WHEN
        List<Task> result = taskService.getTasksByProject(1L, TaskStatus.TODO, null);

        // THEN
        assertEquals(1, result.size());
        assertEquals("Existing task", result.getFirst().getTitle());
        assertEquals("Existing description", result.getFirst().getDescription());
        assertEquals(TaskStatus.TODO, result.getFirst().getStatus());
        assertEquals(TaskPriority.MEDIUM, result.getFirst().getPriority());
        assertEquals(LocalDate.of(2026, 12, 31), result.getFirst().getDueDate());
        assertEquals(otherUser, result.getFirst().getAssignee());
        assertEquals(project, result.getFirst().getProject());
        assertEquals(currentUser, result.getFirst().getProject().getOwner());
        verify(taskRepository).findByProjectIdAndStatus(1L, TaskStatus.TODO);
        verify(taskRepository, never()).findByProjectIdAndPriority(eq(1L), any());
        verify(taskRepository, never()).findByProjectId(1L);
        verify(securityUtils, never()).getCurrentUser();
    }

    @Test
    void getTasksByProject_shouldFilterByPriority() {
        // GIVEN
        when(taskRepository.findByProjectIdAndPriority(1L, TaskPriority.MEDIUM)).thenReturn(List.of(task));

        // WHEN
        List<Task> result = taskService.getTasksByProject(1L, null, TaskPriority.MEDIUM);

        // THEN
        assertEquals(1, result.size());
        assertEquals("Existing task", result.getFirst().getTitle());
        assertEquals("Existing description", result.getFirst().getDescription());
        assertEquals(TaskStatus.TODO, result.getFirst().getStatus());
        assertEquals(TaskPriority.MEDIUM, result.getFirst().getPriority());
        assertEquals(LocalDate.of(2026, 12, 31), result.getFirst().getDueDate());
        assertEquals(otherUser, result.getFirst().getAssignee());
        assertEquals(project, result.getFirst().getProject());
        assertEquals(currentUser, result.getFirst().getProject().getOwner());
        verify(taskRepository, never()).findByProjectIdAndStatus(eq(1L), any());
        verify(taskRepository).findByProjectIdAndPriority(1L, TaskPriority.MEDIUM);
        verify(taskRepository, never()).findByProjectId(1L);
        verify(securityUtils, never()).getCurrentUser();
    }

    @Test
    void getTasksByProject_shouldPrioritizeStatusFilter_whenBothProvided() {
        // GIVEN
        when(taskRepository.findByProjectIdAndStatus(1L, TaskStatus.TODO)).thenReturn(List.of(task));

        // WHEN
        List<Task> result = taskService.getTasksByProject(1L, TaskStatus.TODO, TaskPriority.MEDIUM);

        // THEN
        assertEquals(1, result.size());
        assertEquals("Existing task", result.getFirst().getTitle());
        assertEquals("Existing description", result.getFirst().getDescription());
        assertEquals(TaskStatus.TODO, result.getFirst().getStatus());
        assertEquals(TaskPriority.MEDIUM, result.getFirst().getPriority());
        assertEquals(LocalDate.of(2026, 12, 31), result.getFirst().getDueDate());
        assertEquals(otherUser, result.getFirst().getAssignee());
        assertEquals(project, result.getFirst().getProject());
        assertEquals(currentUser, result.getFirst().getProject().getOwner());
        verify(taskRepository).findByProjectIdAndStatus(1L, TaskStatus.TODO);
        verify(taskRepository, never()).findByProjectIdAndPriority(eq(1L), any());
        verify(taskRepository, never()).findByProjectId(eq(1L));
        verify(securityUtils, never()).getCurrentUser();
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
        assertEquals("Existing task", result.getTitle());
        assertEquals("Existing description", result.getDescription());
        assertEquals(TaskStatus.TODO, result.getStatus());
        assertEquals(TaskPriority.MEDIUM, result.getPriority());
        assertEquals(LocalDate.of(2026, 12, 31), result.getDueDate());
        assertEquals(otherUser, result.getAssignee());
        assertEquals(project, result.getProject());
        assertEquals(currentUser, result.getProject().getOwner());
        verify(securityUtils).getCurrentUser();
        verify(taskRepository).findById(1L);
        verify(taskRepository).existsByIdAndProjectOwnerId(1L, 1L);
        verify(messageService, never()).get(any());
    }

    @Test
    void getTaskById_shouldThrow_whenTaskNotFound() {
        // GIVEN
        givenAuthenticatedUser();
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageService.get("error.task.not.found")).thenReturn("Task not found");

        // WHEN
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> taskService.getTaskById(999L));

        // THEN
        assertEquals("Task not found", ex.getMessage());
        verify(securityUtils).getCurrentUser();
        verify(taskRepository).findById(999L);
        verify(messageService).get("error.task.not.found");
        verify(taskRepository, never()).existsByIdAndProjectOwnerId(999L, 1L);
        verify(messageService, never()).get("error.access.denied");
    }

    @Test
    void getTaskById_shouldThrow_whenNotOwner() {
        // GIVEN
        givenAuthenticatedUser();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.existsByIdAndProjectOwnerId(1L, 1L)).thenReturn(false);
        when(messageService.get("error.access.denied")).thenReturn("Access denied");

        // WHEN
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> taskService.getTaskById(1L));

        // THEN
        assertEquals("Access denied", ex.getMessage());
        verify(securityUtils).getCurrentUser();
        verify(taskRepository).findById(1L);
        verify(messageService, never()).get("error.task.not.found");
        verify(taskRepository).existsByIdAndProjectOwnerId(1L, 1L);
        verify(messageService).get("error.access.denied");
    }

    @Test
    void createTask_shouldCreateAndReturnTask_whenOwner() {
        // GIVEN
        givenAuthenticatedUser();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(sanitizationService.sanitize(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.save(any(Task.class))).thenAnswer(
                invocation -> invocation.getArgument(0));

        // WHEN
        Task result = taskService.createTask(1L, createRequest);

        // THEN
        assertEquals("New task", result.getTitle());
        assertEquals("New description", result.getDescription());
        assertEquals(TaskStatus.TODO, result.getStatus());
        assertEquals(TaskPriority.MEDIUM, result.getPriority());
        assertEquals(LocalDate.of(2026, 12, 31), result.getDueDate());
        assertEquals(otherUser, result.getAssignee());
        assertEquals(project, result.getProject());
        assertEquals(currentUser, result.getProject().getOwner());
        verify(securityUtils).getCurrentUser();
        verify(projectRepository).findById(1L);
        verify(messageService, never()).get(any());
        verify(userRepository).findById(2L);
        verify(taskRepository, times(1)).save(argThat(t ->
                t.getId() == null
                        && t.getTitle().equals("New task")
                        && t.getDescription().equals("New description")
                        && t.getStatus().equals(TaskStatus.TODO)
                        && t.getPriority().equals(TaskPriority.MEDIUM)
                        && t.getDueDate().equals(LocalDate.of(2026, 12, 31))
                        && t.getAssignee().equals(otherUser)
                        && t.getProject().equals(project)
                        && t.getProject().getOwner().equals(currentUser)
        ));
    }

    @Test
    void createTask_shouldCreateAndReturnTask_whenOwnerAndNoAssignee() {
        // GIVEN
        givenAuthenticatedUser();
        createRequest.setAssigneeId(null);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(sanitizationService.sanitize(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.save(any(Task.class))).thenAnswer(
                invocation -> invocation.getArgument(0));

        // WHEN
        Task result = taskService.createTask(1L, createRequest);

        // THEN
        assertEquals("New task", result.getTitle());
        assertEquals("New description", result.getDescription());
        assertEquals(TaskStatus.TODO, result.getStatus());
        assertEquals(TaskPriority.MEDIUM, result.getPriority());
        assertEquals(LocalDate.of(2026, 12, 31), result.getDueDate());
        assertNull(result.getAssignee());
        assertEquals(project, result.getProject());
        assertEquals(currentUser, result.getProject().getOwner());
        verify(securityUtils).getCurrentUser();
        verify(projectRepository).findById(1L);
        verify(messageService, never()).get(any());
        verify(userRepository, never()).findById(any());
        verify(taskRepository, times(1)).save(argThat(t ->
                t.getId() == null
                        && t.getTitle().equals("New task")
                        && t.getDescription().equals("New description")
                        && t.getStatus().equals(TaskStatus.TODO)
                        && t.getPriority().equals(TaskPriority.MEDIUM)
                        && t.getDueDate().equals(LocalDate.of(2026, 12, 31))
                        && t.getAssignee() == null
                        && t.getProject().equals(project)
                        && t.getProject().getOwner().equals(currentUser)
        ));
    }

    @Test
    void createTask_shouldThrow_whenProjectNotFound() {
        // GIVEN
        givenAuthenticatedUser();
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageService.get("error.project.not.found")).thenReturn("Project not found");

        // WHEN
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> taskService.createTask(999L, createRequest));

        // THEN
        assertEquals("Project not found", ex.getMessage());
        verify(securityUtils).getCurrentUser();
        verify(projectRepository).findById(999L);
        verify(messageService).get("error.project.not.found");
        verify(messageService, never()).get("error.access.denied");
        verify(userRepository, never()).findById(any());
        verify(messageService, never()).get("error.assignee.not.found");
        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_shouldThrow_whenNotProjectOwner() {
        // GIVEN
        givenAuthenticatedUser();
        project.setOwner(otherUser);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(messageService.get("error.access.denied")).thenReturn("Access denied");

        // WHEN
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> taskService.createTask(1L, createRequest));
        // THEN
        assertEquals("Access denied", ex.getMessage());
        verify(securityUtils).getCurrentUser();
        verify(projectRepository).findById(1L);
        verify(messageService, never()).get("error.project.not.found");
        verify(messageService).get("error.access.denied");
        verify(userRepository, never()).findById(any());
        verify(messageService, never()).get("error.assignee.not.found");
        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_shouldThrow_whenUserAssignedNotFound() {
        // GIVEN
        givenAuthenticatedUser();
        createRequest.setAssigneeId(99L);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        when(messageService.get("error.assignee.not.found")).thenReturn("Assignee not found");

        // WHEN
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> taskService.createTask(1L, createRequest));

        // THEN
        assertEquals("Assignee not found", ex.getMessage());
        verify(securityUtils).getCurrentUser();
        verify(projectRepository).findById(1L);
        verify(messageService, never()).get("error.project.not.found");
        verify(messageService, never()).get("error.access.denied");
        verify(userRepository).findById(99L);
        verify(messageService).get("error.assignee.not.found");
        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateTask_shouldUpdateAndReturnTask_whenOwner() {
        // GIVEN
        givenAuthenticatedUser();
        when(taskRepository.existsByIdAndProjectOwnerId(1L, 1L)).thenReturn(true);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(sanitizationService.sanitize(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // WHEN
        Task result = taskService.updateTask(1L, updateRequest);

        // THEN
        assertEquals("Updated task", result.getTitle());
        assertEquals("Updated description", result.getDescription());
        assertEquals(TaskStatus.TODO, result.getStatus());
        assertEquals(TaskPriority.MEDIUM, result.getPriority());
        assertEquals(LocalDate.of(2026, 12, 31), result.getDueDate());
        assertEquals(otherUser, result.getAssignee());
        assertEquals(project, result.getProject());
        assertEquals(currentUser, result.getProject().getOwner());
        verify(securityUtils).getCurrentUser();
        verify(taskRepository).existsByIdAndProjectOwnerId(1L, 1L);
        verify(taskRepository).findById(1L);
        verify(userRepository).findById(2L);
        verify(messageService, never()).get(any());
        verify(taskRepository).save(argThat(t ->
                t.getId().equals(1L)
                        && t.getTitle().equals("Updated task")
                        && t.getDescription().equals("Updated description")
                        && t.getStatus().equals(TaskStatus.TODO)
                        && t.getPriority().equals(TaskPriority.MEDIUM)
                        && t.getDueDate().equals(LocalDate.of(2026, 12, 31))
                        && t.getAssignee().equals(otherUser)
                        && t.getProject().equals(project)
                        && t.getProject().getOwner().equals(currentUser)
                        && t.getCreatedAt() == null
        ));
    }

    @Test
    void updateTask_shouldUpdateAndReturnTask_whenOwnerAndNoAssignee() {
        // GIVEN
        givenAuthenticatedUser();
        updateRequest.setAssigneeId(null);
        task.setAssignee(null);
        when(taskRepository.existsByIdAndProjectOwnerId(1L, 1L)).thenReturn(true);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(sanitizationService.sanitize(any())).thenAnswer(invocation -> invocation.getArgument(0));;
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // WHEN
        Task result = taskService.updateTask(1L, updateRequest);

        // THEN
        assertEquals("Updated task", result.getTitle());
        assertEquals("Updated description", result.getDescription());
        assertEquals(TaskStatus.TODO, result.getStatus());
        assertEquals(TaskPriority.MEDIUM, result.getPriority());
        assertEquals(LocalDate.of(2026, 12, 31), result.getDueDate());
        assertNull(result.getAssignee());
        assertEquals(project, result.getProject());
        assertEquals(currentUser, result.getProject().getOwner());
        verify(securityUtils).getCurrentUser();
        verify(taskRepository).existsByIdAndProjectOwnerId(1L, 1L);
        verify(taskRepository).findById(1L);
        verify(userRepository, never()).findById(2L);
        verify(messageService, never()).get(any());
        verify(taskRepository).save(argThat(t ->
                t.getId().equals(1L)
                        && t.getTitle().equals("Updated task")
                        && t.getDescription().equals("Updated description")
                        && t.getStatus().equals(TaskStatus.TODO)
                        && t.getPriority().equals(TaskPriority.MEDIUM)
                        && t.getDueDate().equals(LocalDate.of(2026, 12, 31))
                        && t.getAssignee() == null
                        && t.getProject().equals(project)
                        && t.getProject().getOwner().equals(currentUser)
                        && t.getCreatedAt() == null
        ));
    }

    @Test
    void updateTask_shouldThrow_whenNotOwner() {
        // GIVEN
        givenAuthenticatedUser();
        when(taskRepository.existsByIdAndProjectOwnerId(1L, 1L)).thenReturn(false);
        when(messageService.get("error.access.denied")).thenReturn("Access denied");

        // WHEN
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> taskService.updateTask(1L, updateRequest));

        // THEN
        assertEquals("Access denied", ex.getMessage());
        verify(securityUtils).getCurrentUser();
        verify(taskRepository).existsByIdAndProjectOwnerId(1L, 1L);
        verify(messageService).get("error.access.denied");
        verify(taskRepository, never()).findById(1L);
        verify(messageService, never()).get("error.task.not.found");
        verify(userRepository, never()).findById(2L);
        verify(messageService, never()).get("error.assignee.not.found");
        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateTask_shouldThrow_whenTaskNotFound() {
        // GIVEN
        givenAuthenticatedUser();
        when(taskRepository.existsByIdAndProjectOwnerId(1L, 1L)).thenReturn(true);
        when(taskRepository.findById(1L)).thenReturn(Optional.empty());
        when(messageService.get("error.task.not.found")).thenReturn("Task not found");

        // WHEN
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> taskService.updateTask(1L, updateRequest));

        // THEN
        assertEquals("Task not found", ex.getMessage());
        verify(securityUtils).getCurrentUser();
        verify(taskRepository).existsByIdAndProjectOwnerId(1L, 1L);
        verify(messageService, never()).get("error.access.denied");
        verify(taskRepository).findById(1L);
        verify(messageService).get("error.task.not.found");
        verify(userRepository, never()).findById(2L);
        verify(messageService, never()).get("error.assignee.not.found");
        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateTask_shouldThrow_whenUserAssignedNotFound() {
        // GIVEN
        givenAuthenticatedUser();
        updateRequest.setAssigneeId(99L);
        when(taskRepository.existsByIdAndProjectOwnerId(1L, 1L)).thenReturn(true);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        when(messageService.get("error.assignee.not.found")).thenReturn("Assignee not found");

        // WHEN
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> taskService.updateTask(1L, updateRequest));

        // THEN
        assertEquals("Assignee not found", ex.getMessage());
        verify(securityUtils).getCurrentUser();
        verify(taskRepository).existsByIdAndProjectOwnerId(1L, 1L);
        verify(messageService, never()).get("error.access.denied");
        verify(taskRepository).findById(1L);
        verify(messageService, never()).get("error.task.not.found");
        verify(userRepository).findById(99L);
        verify(messageService).get("error.assignee.not.found");
        verify(taskRepository, never()).save(any());
    }

    @Test
    void deleteTask_shouldDelete_whenOwner() {
        // GIVEN
        givenAuthenticatedUser();
        when(taskRepository.existsByIdAndProjectOwnerId(1L, 1L)).thenReturn(true);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        // WHEN
        taskService.deleteTask(1L);

        // THEN
        verify(securityUtils).getCurrentUser();
        verify(taskRepository).existsByIdAndProjectOwnerId(1L, 1L);
        verify(taskRepository).findById(1L);
        verify(messageService, never()).get(any());
        verify(auditService).logTaskDeletion(1L, "mehdi");
        verify(taskRepository).delete(task);
    }

    @Test
    void deleteTask_shouldThrow_whenNotOwner() {
        // GIVEN
        givenAuthenticatedUser();
        when(taskRepository.existsByIdAndProjectOwnerId(1L, 1L)).thenReturn(false);
        when(messageService.get("error.access.denied")).thenReturn("Access denied");

        // WHEN
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> taskService.deleteTask(1L));

        // THEN
        assertEquals("Access denied", ex.getMessage());
        verify(securityUtils).getCurrentUser();
        verify(taskRepository).existsByIdAndProjectOwnerId(1L, 1L);
        verify(messageService).get("error.access.denied");
        verify(taskRepository, never()).findById(1L);
        verify(messageService, never()).get("error.task.not.found");
        verify(taskRepository, never()).delete(task);
    }

    @Test
    void deleteTask_shouldThrow_whenTaskNotFound() {
        // GIVEN
        givenAuthenticatedUser();
        when(taskRepository.existsByIdAndProjectOwnerId(1L, 1L)).thenReturn(true);
        when(taskRepository.findById(1L)).thenReturn(Optional.empty());
        when(messageService.get("error.task.not.found")).thenReturn("Task not found");

        // WHEN
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> taskService.deleteTask(1L));

        // THEN
        assertEquals("Task not found", ex.getMessage());
        verify(securityUtils).getCurrentUser();
        verify(taskRepository).existsByIdAndProjectOwnerId(1L, 1L);
        verify(messageService, never()).get("error.access.denied");
        verify(taskRepository).findById(1L);
        verify(messageService).get("error.task.not.found");
        verify(taskRepository, never()).delete(task);
    }
}