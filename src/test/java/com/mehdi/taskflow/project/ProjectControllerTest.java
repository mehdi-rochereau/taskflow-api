package com.mehdi.taskflow.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mehdi.taskflow.config.MessageService;
import com.mehdi.taskflow.config.SecurityConfig;
import com.mehdi.taskflow.exception.ResourceNotFoundException;
import com.mehdi.taskflow.project.dto.ProjectRequest;
import com.mehdi.taskflow.security.JwtFilter;
import com.mehdi.taskflow.security.JwtService;
import com.mehdi.taskflow.security.UserDetailsServiceImpl;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "mehdi")
public class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjectService projectService;

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

    private Project project;
    private ProjectRequest projectRequest;

    @BeforeEach
    void setUp() throws Exception {

        Locale.setDefault(Locale.ENGLISH);

        User owner = new User();
        owner.setId(1L);
        owner.setUsername("mehdi");

        project = new Project();
        project.setId(1L);
        project.setName("My project");
        project.setDescription("Description");
        project.setOwner(owner);
        project.setCreatedAt(LocalDateTime.now());

        projectRequest = new ProjectRequest();
        projectRequest.setName("My project");
        projectRequest.setDescription("Description");

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    @Test
    void getMyProjects_shouldReturn200_withProjectList() throws Exception {
        // GIVEN
        when(projectService.getMyProjects()).thenReturn(List.of(project));

        // WHEN & THEN
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("My project"))
                .andExpect(jsonPath("$[0].description").value("Description"))
                .andExpect(jsonPath("$[0].ownerUsername").value("mehdi"));
    }

    @Test
    void getMyProjects_shouldReturn200_withEmptyList() throws Exception {
        // GIVEN
        when(projectService.getMyProjects()).thenReturn(List.of());

        // WHEN & THEN
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getProjectById_shouldReturn200_whenFound() throws Exception {
        // GIVEN
        when(projectService.getProjectById(1L)).thenReturn(project);

        // WHEN & THEN
        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("My project"))
                .andExpect(jsonPath("$.description").value("Description"))
                .andExpect(jsonPath("$.ownerUsername").value("mehdi"));
    }

    @Test
    @WithAnonymousUser
    void getMyProjects_shouldReturn401_whenNotAuthenticated() throws Exception {
        // GIVEN
        when(messageService.get("error.authentication.required"))
                .thenReturn("Authentication required");

        // WHEN & THEN
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void getProjectById_shouldReturn404_whenNotFound() throws Exception {
        // GIVEN
        when(projectService.getProjectById(999L))
                .thenThrow(new ResourceNotFoundException("Project not found"));

        // WHEN & THEN
        mockMvc.perform(get("/api/projects/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project not found"));
    }

    @Test
    void getProjectById_shouldReturn403_whenNotOwner() throws Exception {
        // GIVEN
        when(messageService.get("error.access.denied")).thenReturn("Access denied");
        when(projectService.getProjectById(1L))
                .thenThrow(new AccessDeniedException("Access denied"));

        // WHEN & THEN
        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void getProjectById_shouldReturn400_whenWrongParameterType() throws Exception {
        // GIVEN
        when(messageService.get("error.parameter.type.mismatch", "id", "Long"))
                .thenReturn("Parameter 'id' must be of type Long");

        // WHEN & THEN
        mockMvc.perform(get("/api/projects/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'id' must be of type Long"));
    }

    @Test
    @WithAnonymousUser
    void getProjectById_shouldReturn401_whenNotAuthenticated() throws Exception {
        // GIVEN
        when(messageService.get("error.authentication.required"))
                .thenReturn("Authentication required");

        // WHEN & THEN
        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void createProject_shouldReturn201_whenValid() throws Exception {
        // GIVEN
        when(projectService.createProject(any(ProjectRequest.class))).thenReturn(project);

        // WHEN & THEN
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("My project"))
                .andExpect(jsonPath("$.description").value("Description"))
                .andExpect(jsonPath("$.ownerUsername").value("mehdi"));
    }

    @Test
    void createProject_shouldReturn400_whenNameIsBlank() throws Exception {
        // GIVEN
        projectRequest.setName("");

        // WHEN & THEN
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.name").isArray())
                .andExpect(jsonPath("$.errors.name",
                        Matchers.contains("Project name is required")));
    }

    @Test
    void createProject_shouldReturn400_whenNameIsTooLong() throws Exception {
        // GIVEN
        projectRequest.setName("a".repeat(101));

        // WHEN & THEN
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.name").isArray())
                .andExpect(jsonPath("$.errors.name",
                        Matchers.contains("Project name must not exceed 100 characters")));
    }

    @Test
    void createProject_shouldReturn400_whenDescriptionIsTooLong() throws Exception {
        // GIVEN
        projectRequest.setDescription("a".repeat(501));

        // WHEN & THEN
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.description").isArray())
                .andExpect(jsonPath("$.errors.description",
                        Matchers.contains("Project description must not exceed 500 characters")));
    }

    @Test
    @WithAnonymousUser
    void createProject_shouldReturn401_whenNotAuthenticated() throws Exception {
        // GIVEN
        when(messageService.get("error.authentication.required"))
                .thenReturn("Authentication required");

        // WHEN & THEN
        mockMvc.perform(post("/api/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void updateProject_shouldReturn200_whenValid() throws Exception {
        // GIVEN
        when(projectService.updateProject(eq(1L), any(ProjectRequest.class))).thenReturn(project);

        // WHEN & THEN
        mockMvc.perform(put("/api/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("My project"))
                .andExpect(jsonPath("$.description").value("Description"))
                .andExpect(jsonPath("$.ownerUsername").value("mehdi"));
    }

    @Test
    void updateProject_shouldReturn400_whenNameIsBlank() throws Exception {
        // GIVEN
        projectRequest.setName("");

        // WHEN & THEN
        mockMvc.perform(put("/api/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.name").isArray())
                .andExpect(jsonPath("$.errors.name",
                        Matchers.contains("Project name is required")));
    }

    @Test
    void updateProject_shouldReturn400_whenNameIsTooLong() throws Exception {
        // GIVEN
        projectRequest.setName("a".repeat(101));

        // WHEN & THEN
        mockMvc.perform(put("/api/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.name").isArray())
                .andExpect(jsonPath("$.errors.name",
                        Matchers.contains("Project name must not exceed 100 characters")));
    }

    @Test
    void updateProject_shouldReturn400_whenDescriptionIsTooLong() throws Exception {
        // GIVEN
        projectRequest.setDescription("a".repeat(501));

        // WHEN & THEN
        mockMvc.perform(put("/api/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.description").isArray())
                .andExpect(jsonPath("$.errors.description",
                        Matchers.contains("Project description must not exceed 500 characters")));
    }

    @Test
    void updateProject_shouldReturn403_whenNotOwner() throws Exception {
        // GIVEN
        when(messageService.get("error.access.denied")).thenReturn("Access denied");
        when(projectService.updateProject(eq(1L), any(ProjectRequest.class)))
                .thenThrow(new AccessDeniedException("Access denied"));

        // WHEN & THEN
        mockMvc.perform(put("/api/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void updateProject_shouldReturn404_whenNotFound() throws Exception {
        // GIVEN
        when(projectService.updateProject(eq(999L), any(ProjectRequest.class)))
                .thenThrow(new ResourceNotFoundException("Project not found"));

        // WHEN & THEN
        mockMvc.perform(put("/api/projects/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project not found"));
    }

    @Test
    void updateProject_shouldReturn400_whenWrongParameterType() throws Exception {
        // GIVEN
        when(messageService.get("error.parameter.type.mismatch", "id", "Long"))
                .thenReturn("Parameter 'id' must be of type Long");

        // WHEN & THEN
        mockMvc.perform(put("/api/projects/abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'id' must be of type Long"));
    }

    @Test
    @WithAnonymousUser
    void updateProject_shouldReturn401_whenNotAuthenticated() throws Exception {
        // GIVEN
        when(messageService.get("error.authentication.required"))
                .thenReturn("Authentication required");

        // WHEN & THEN
        mockMvc.perform(put("/api/projects/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void deleteProject_shouldReturn204_whenOwner() throws Exception {
        // WHEN & THEN
        mockMvc.perform(delete("/api/projects/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteProject_shouldReturn403_whenNotOwner() throws Exception {
        // GIVEN
        when(messageService.get("error.access.denied")).thenReturn("Access denied");
        doThrow(new AccessDeniedException("Access denied"))
                .when(projectService).deleteProject(1L);

        // WHEN & THEN
        mockMvc.perform(delete("/api/projects/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void deleteProject_shouldReturn404_whenNotFound() throws Exception {
        // GIVEN
        doThrow(new ResourceNotFoundException("Project not found"))
                .when(projectService).deleteProject(1L);

        // WHEN & THEN
        mockMvc.perform(delete("/api/projects/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project not found"));
    }

    @Test
    void deleteProject_shouldReturn400_whenWrongParameterType() throws Exception {
        // GIVEN
        when(messageService.get("error.parameter.type.mismatch", "id", "Long")).thenReturn("Parameter 'id' must be of type Long");

        // WHEN & THEN
        mockMvc.perform(delete("/api/projects/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'id' must be of type Long"));
    }

    @Test
    @WithAnonymousUser
    void deleteProject_shouldReturn401_whenNotAuthenticated() throws Exception {
        // GIVEN
        when(messageService.get("error.authentication.required"))
                .thenReturn("Authentication required");

        // WHEN & THEN
        mockMvc.perform(delete("/api/projects/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }
}
