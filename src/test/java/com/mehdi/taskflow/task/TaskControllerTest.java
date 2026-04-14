package com.mehdi.taskflow.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mehdi.taskflow.config.MessageService;
import com.mehdi.taskflow.config.SecurityConfig;
import com.mehdi.taskflow.exception.ResourceNotFoundException;
import com.mehdi.taskflow.project.Project;
import com.mehdi.taskflow.security.JwtFilter;
import com.mehdi.taskflow.security.JwtService;
import com.mehdi.taskflow.security.UserDetailsServiceImpl;
import com.mehdi.taskflow.task.dto.TaskRequest;
import com.mehdi.taskflow.user.User;
import jakarta.servlet.FilterChain;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "mehdi")
public class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private MessageService messageService;

    private Task task;
    private TaskRequest taskRequest;

    @BeforeEach
    void setUp() throws Exception {

        Locale.setDefault(Locale.ENGLISH);

        User owner = new User();
        owner.setId(1L);
        owner.setUsername("mehdi");

        Project project = new Project();
        project.setId(1L);
        project.setName("My project");

        task = new Task();
        task.setId(1L);
        task.setTitle("My task");
        task.setDescription("Description");
        task.setStatus(TaskStatus.TODO);
        task.setPriority(TaskPriority.MEDIUM);
        task.setCreatedAt(LocalDateTime.now());
        task.setAssignee(owner);
        task.setProject(project);

        taskRequest = new TaskRequest();
        taskRequest.setTitle("My task");
        taskRequest.setStatus(TaskStatus.TODO);
        taskRequest.setPriority(TaskPriority.MEDIUM);

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    @Test
    void getTasksByProject_shouldReturn200_withTaskList() throws Exception {
        // GIVEN
        when(taskService.getTasksByProject(1L, null, null))
                .thenReturn(List.of(task));

        // WHEN & THEN
        mockMvc.perform(get("/api/projects/1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("My task"))
                .andExpect(jsonPath("$[0].description").value("Description"))
                .andExpect(jsonPath("$[0].status").value("TODO"))
                .andExpect(jsonPath("$[0].priority").value("MEDIUM"))
                .andExpect(jsonPath("$[0].projectId").value(1))
                .andExpect(jsonPath("$[0].assigneeUsername").value("mehdi"));
    }

    @Test
    void getTasksByProject_shouldReturn200_withEmptyList() throws Exception {
        // GIVEN
        when(taskService.getTasksByProject(1L, null, null))
                .thenReturn(List.of());

        // WHEN & THEN
        mockMvc.perform(get("/api/projects/1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getTasksByProject_shouldFilterByStatus() throws Exception {
        // GIVEN
        when(taskService.getTasksByProject(1L, TaskStatus.TODO, null))
                .thenReturn(List.of(task));

        // WHEN & THEN
        mockMvc.perform(get("/api/projects/1/tasks?status=TODO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("My task"))
                .andExpect(jsonPath("$[0].description").value("Description"))
                .andExpect(jsonPath("$[0].status").value("TODO"))
                .andExpect(jsonPath("$[0].priority").value("MEDIUM"))
                .andExpect(jsonPath("$[0].projectId").value(1))
                .andExpect(jsonPath("$[0].assigneeUsername").value("mehdi"));
    }

    @Test
    void getTasksByProject_shouldFilterByPriority() throws Exception {
        // GIVEN
        when(taskService.getTasksByProject(1L, null, TaskPriority.MEDIUM))
                .thenReturn(List.of(task));

        // WHEN & THEN
        mockMvc.perform(get("/api/projects/1/tasks?priority=MEDIUM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("My task"))
                .andExpect(jsonPath("$[0].description").value("Description"))
                .andExpect(jsonPath("$[0].status").value("TODO"))
                .andExpect(jsonPath("$[0].priority").value("MEDIUM"))
                .andExpect(jsonPath("$[0].projectId").value(1))
                .andExpect(jsonPath("$[0].assigneeUsername").value("mehdi"));
    }

    @Test
    void getTasksByProject_shouldPrioritizeStatusFilter_whenBothProvided() throws Exception {
        // GIVEN
        when(taskService.getTasksByProject(1L, TaskStatus.TODO, TaskPriority.MEDIUM))
                .thenReturn(List.of(task));

        // WHEN & THEN
        mockMvc.perform(get("/api/projects/1/tasks?status=TODO&priority=MEDIUM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("My task"))
                .andExpect(jsonPath("$[0].description").value("Description"))
                .andExpect(jsonPath("$[0].status").value("TODO"))
                .andExpect(jsonPath("$[0].priority").value("MEDIUM"))
                .andExpect(jsonPath("$[0].projectId").value(1))
                .andExpect(jsonPath("$[0].assigneeUsername").value("mehdi"));
    }

    @Test
    void getTasksByProject_shouldReturn400_whenStatusIsInvalid() throws Exception {
        // GIVEN
        when(messageService.get("error.parameter.type.mismatch", "status", "TaskStatus"))
                .thenReturn("Parameter 'status' must be of type TaskStatus");

        // WHEN & THEN
        mockMvc.perform(get("/api/projects/1/tasks?status=INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'status' must be of type TaskStatus"));
    }

    @Test
    void getTasksByProject_shouldReturn400_whenPriorityIsInvalid() throws Exception {
        // GIVEN
        when(messageService.get("error.parameter.type.mismatch", "priority", "TaskPriority"))
                .thenReturn("Parameter 'priority' must be of type TaskPriority");

        // WHEN & THEN
        mockMvc.perform(get("/api/projects/1/tasks?priority=INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'priority' must be of type TaskPriority"));
    }

    @Test
    void getTasksByProject_shouldReturn400_whenProjectIdIsInvalid() throws Exception {
        // GIVEN
        when(messageService.get("error.parameter.type.mismatch", "projectId", "Long"))
                .thenReturn("Parameter 'projectId' must be of type Long");

        // WHEN & THEN
        mockMvc.perform(get("/api/projects/abc/tasks"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'projectId' must be of type Long"));
    }

    @Test
    @WithAnonymousUser
    void getTasksByProject_shouldReturn401_whenUnauthorized() throws Exception {
        // GIVEN
        when(messageService.get("error.authentication.required"))
                .thenReturn("Authentication required");

        // WHEN & THEN
        mockMvc.perform(get("/api/projects/1/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void getTaskById_shouldReturn200_whenOwner() throws Exception {
        // GIVEN
        when(taskService.getTaskById(1L)).thenReturn(task);

        // WHEN & THEN
        mockMvc.perform(get("/api/projects/1/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("My task"))
                .andExpect(jsonPath("$.description").value("Description"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.projectId").value(1))
                .andExpect(jsonPath("$.assigneeUsername").value("mehdi"));
    }

    @Test
    void getTaskById_shouldReturn404_whenNotFound() throws Exception {
        // GIVEN
        when(taskService.getTaskById(999L))
                .thenThrow(new ResourceNotFoundException("Task not found"));

        // WHEN & THEN
        mockMvc.perform(get("/api/projects/1/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found"));
    }

    @Test
    void getTaskById_shouldReturn403_whenNotProjectOwner() throws Exception {
        // GIVEN
        when(messageService.get("error.access.denied")).thenReturn("Access denied");
        when(taskService.getTaskById(1L))
                .thenThrow(new AccessDeniedException("Access denied"));

        // WHEN & THEN
        mockMvc.perform(get("/api/projects/1/tasks/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void getTaskById_shouldReturn400_whenIdIsInvalid() throws Exception {
        // GIVEN
        when(messageService.get("error.parameter.type.mismatch", "id", "Long"))
                .thenReturn("Parameter 'id' must be of type Long");

        // WHEN & THEN
        mockMvc.perform(get("/api/projects/1/tasks/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'id' must be of type Long"));
    }

    @Test
    void getTaskById_shouldReturn400_whenProjectIdIsInvalid() throws Exception {
        // GIVEN
        when(messageService.get("error.parameter.type.mismatch", "projectId", "Long"))
                .thenReturn("Parameter 'projectId' must be of type Long");

        // WHEN & THEN
        mockMvc.perform(get("/api/projects/abc/tasks/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'projectId' must be of type Long"));
    }

    @Test
    @WithAnonymousUser
    void getTaskById_shouldReturn401_whenUnauthorized() throws Exception {
        // GIVEN
        when(messageService.get("error.authentication.required"))
                .thenReturn("Authentication required");

        // WHEN & THEN
        mockMvc.perform(get("/api/projects/1/tasks/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void createTask_shouldCreateAndReturn201_whenValid() throws Exception {
        // GIVEN
        when(taskService.createTask(eq(1L), any(TaskRequest.class))).thenReturn(task);

        // WHEN & THEN
        mockMvc.perform(post("/api/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("My task"))
                .andExpect(jsonPath("$.description").value("Description"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.projectId").value(1))
                .andExpect(jsonPath("$.assigneeUsername").value("mehdi"));
    }

    @Test
    void createTask_shouldReturn400_whenTitleIsBlank() throws Exception {
        // GIVEN
        taskRequest.setTitle("");

        // WHEN & THEN
        mockMvc.perform(post("/api/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.title").isArray())
                .andExpect(jsonPath("$.errors.title",
                        Matchers.contains("Task title is required")));
    }

    @Test
    void createTask_shouldReturn400_whenTitleIsTooLong() throws Exception {
        // GIVEN
        taskRequest.setTitle("a".repeat(201));

        // WHEN & THEN
        mockMvc.perform(post("/api/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.title").isArray())
                .andExpect(jsonPath("$.errors.title",
                        Matchers.contains("Task title must not exceed 200 characters")));
    }

    @Test
    void createTask_shouldReturn400_whenDescriptionIsTooLong() throws Exception {
        // GIVEN
        taskRequest.setDescription("a".repeat(1001));

        // WHEN & THEN
        mockMvc.perform(post("/api/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.description").isArray())
                .andExpect(jsonPath("$.errors.description",
                        Matchers.contains("Task description must not exceed 1000 characters")));
    }

    @Test
    void createTask_shouldReturn400_whenStatusIsNull() throws Exception {
        // GIVEN
        taskRequest.setStatus(null);

        // WHEN & THEN
        mockMvc.perform(post("/api/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status").exists())
                .andExpect(jsonPath("$.errors.status").isArray())
                .andExpect(jsonPath("$.errors.status",
                        Matchers.contains("Task status is required")));
    }

    @Test
    void createTask_shouldReturn400_whenPriorityIsNull() throws Exception {
        // GIVEN
        taskRequest.setPriority(null);

        // WHEN & THEN
        mockMvc.perform(post("/api/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.priority").exists())
                .andExpect(jsonPath("$.errors.priority").isArray())
                .andExpect(jsonPath("$.errors.priority",
                        Matchers.contains("Task priority is required")));
    }

    @Test
    void createTask_shouldReturn404_whenProjectNotFound() throws Exception {
        // GIVEN
        when(taskService.createTask(eq(999L), any(TaskRequest.class)))
                .thenThrow(new ResourceNotFoundException("Project not found"));

        // WHEN & THEN
        mockMvc.perform(post("/api/projects/999/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project not found"));
    }

    @Test
    void createTask_shouldReturn404_whenAssigneeNotFound() throws Exception {
        // GIVEN
        taskRequest.setAssigneeId(99L);
        when(taskService.createTask(eq(1L), any(TaskRequest.class)))
                .thenThrow(new ResourceNotFoundException("Assignee not found"));

        // WHEN & THEN
        mockMvc.perform(post("/api/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Assignee not found"));
    }

    @Test
    void createTask_shouldReturn403_whenNotProjectOwner() throws Exception {
        // GIVEN
        when(messageService.get("error.access.denied")).thenReturn("Access denied");
        when(taskService.createTask(eq(2L), any(TaskRequest.class)))
                .thenThrow(new AccessDeniedException("Access denied"));

        // WHEN & THEN
        mockMvc.perform(post("/api/projects/2/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void createTask_shouldReturn400_whenProjectIdIsInvalid() throws Exception {
        // GIVEN
        when(messageService.get("error.parameter.type.mismatch", "projectId", "Long"))
                .thenReturn("Parameter 'projectId' must be of type Long");

        // WHEN & THEN
        mockMvc.perform(post("/api/projects/abc/tasks"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'projectId' must be of type Long"));
    }

    @Test
    @WithAnonymousUser
    void createTask_shouldReturn401_whenUnauthorized() throws Exception {
        // GIVEN
        when(messageService.get("error.authentication.required"))
                .thenReturn("Authentication required");

        // WHEN & THEN
        mockMvc.perform(post("/api/projects/1/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void updateTask_shouldUpdateAndReturn200_whenValid() throws Exception {
        // GIVEN
        when(taskService.updateTask(eq(1L), any(TaskRequest.class))).thenReturn(task);

        // WHEN & THEN
        mockMvc.perform(put("/api/projects/1/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("My task"))
                .andExpect(jsonPath("$.description").value("Description"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.projectId").value(1))
                .andExpect(jsonPath("$.assigneeUsername").value("mehdi"));
    }

    @Test
    void updateTask_shouldReturn400_whenTitleIsBlank() throws Exception {
        // GIVEN
        taskRequest.setTitle("");

        // WHEN & THEN
        mockMvc.perform(put("/api/projects/1/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.title").isArray())
                .andExpect(jsonPath("$.errors.title", Matchers.contains("Task title is required")));
    }

    @Test
    void updateTask_shouldReturn400_whenTitleIsTooLong() throws Exception {
        // GIVEN
        taskRequest.setTitle("a".repeat(201));

        // WHEN & THEN
        mockMvc.perform(put("/api/projects/1/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.title").isArray())
                .andExpect(jsonPath("$.errors.title", Matchers.contains("Task title must not exceed 200 characters")));
    }

    @Test
    void updateTask_shouldReturn400_whenDescriptionIsTooLong() throws Exception {
        // GIVEN
        taskRequest.setDescription("a".repeat(1001));

        // WHEN & THEN
        mockMvc.perform(put("/api/projects/1/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.description").isArray())
                .andExpect(jsonPath("$.errors.description", Matchers.contains("Task description must not exceed 1000 characters")));
    }

    @Test
    void updateTask_shouldReturn400_whenStatusIsNull() throws Exception {
        // GIVEN
        taskRequest.setStatus(null);

        // WHEN & THEN
        mockMvc.perform(put("/api/projects/1/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status").exists())
                .andExpect(jsonPath("$.errors.status").isArray())
                .andExpect(jsonPath("$.errors.status", Matchers.contains("Task status is required")));
    }

    @Test
    void updateTask_shouldReturn400_whenPriorityIsNull() throws Exception {
        // GIVEN
        taskRequest.setPriority(null);

        // WHEN & THEN
        mockMvc.perform(put("/api/projects/1/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.priority").exists())
                .andExpect(jsonPath("$.errors.priority").isArray())
                .andExpect(jsonPath("$.errors.priority", Matchers.contains("Task priority is required")));
    }

    @Test
    void updateTask_shouldReturn403_whenNotOwner() throws Exception {
        // GIVEN
        when(messageService.get("error.access.denied")).thenReturn("Access denied");
        when(taskService.updateTask(eq(1L), any(TaskRequest.class)))
                .thenThrow(new AccessDeniedException("Access denied"));

        // WHEN & THEN
        mockMvc.perform(put("/api/projects/1/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void updateTask_shouldReturn404_whenTaskNotFound() throws Exception {
        // GIVEN
        when(taskService.updateTask(eq(999L), any(TaskRequest.class)))
                .thenThrow(new ResourceNotFoundException("Task not found"));

        // WHEN & THEN
        mockMvc.perform(put("/api/projects/1/tasks/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found"));
    }

    @Test
    void updateTask_shouldReturn404_whenAssigneeNotFound() throws Exception {
        // GIVEN
        taskRequest.setAssigneeId(99L);
        when(taskService.updateTask(eq(1L), any(TaskRequest.class)))
                .thenThrow(new ResourceNotFoundException("Assignee not found"));

        // WHEN & THEN
        mockMvc.perform(put("/api/projects/1/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Assignee not found"));
    }

    @Test
    void updateTask_shouldReturn400_whenIdIsInvalid() throws Exception {
        // GIVEN
        when(messageService.get("error.parameter.type.mismatch", "id", "Long"))
                .thenReturn("Parameter 'id' must be of type Long");

        // WHEN & THEN
        mockMvc.perform(put("/api/projects/1/tasks/abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'id' must be of type Long"));
    }

    @Test
    void updateTask_shouldReturn400_whenProjectIdIsInvalid() throws Exception {
        // GIVEN
        when(messageService.get("error.parameter.type.mismatch", "projectId", "Long"))
                .thenReturn("Parameter 'projectId' must be of type Long");

        // WHEN & THEN
        mockMvc.perform(put("/api/projects/abc/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'projectId' must be of type Long"));
    }

    @Test
    @WithAnonymousUser
    void updateTask_shouldReturn401_whenUnauthorized() throws Exception {
        // GIVEN
        when(messageService.get("error.authentication.required"))
                .thenReturn("Authentication required");

        // WHEN & THEN
        mockMvc.perform(put("/api/projects/1/tasks/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void deleteTask_shouldDeleteAndReturn204() throws Exception {
        // WHEN & THEN
        mockMvc.perform(delete("/api/projects/1/tasks/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTask_shouldReturn403_whenNotOwner() throws Exception {
        // GIVEN
        when(messageService.get("error.access.denied")).thenReturn("Access denied");
        doThrow(new AccessDeniedException("Access denied"))
                .when(taskService).deleteTask(1L);

        // WHEN & THEN
        mockMvc.perform(delete("/api/projects/1/tasks/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void deleteTask_shouldReturn404_whenNotFound() throws Exception {
        // GIVEN
        doThrow(new ResourceNotFoundException("Task not found"))
                .when(taskService).deleteTask(1L);

        // WHEN & THEN
        mockMvc.perform(delete("/api/projects/1/tasks/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found"));
    }

    @Test
    void deleteTask_shouldReturn400_whenIdIsInvalid() throws Exception {
        // GIVEN
        when(messageService.get("error.parameter.type.mismatch", "id", "Long"))
                .thenReturn("Parameter 'id' must be of type Long");

        // WHEN & THEN
        mockMvc.perform(delete("/api/projects/1/tasks/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'id' must be of type Long"));
    }

    @Test
    void deleteTask_shouldReturn400_whenProjectIdIsInvalid() throws Exception {
        // GIVEN
        when(messageService.get("error.parameter.type.mismatch", "projectId", "Long"))
                .thenReturn("Parameter 'projectId' must be of type Long");

        // WHEN & THEN
        mockMvc.perform(delete("/api/projects/abc/tasks/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'projectId' must be of type Long"));
    }

    @Test
    @WithAnonymousUser
    void deleteTask_shouldReturn401_whenUnauthorized() throws Exception {
        // GIVEN
        when(messageService.get("error.authentication.required"))
                .thenReturn("Authentication required");

        // WHEN & THEN
        mockMvc.perform(delete("/api/projects/1/tasks/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }
}
