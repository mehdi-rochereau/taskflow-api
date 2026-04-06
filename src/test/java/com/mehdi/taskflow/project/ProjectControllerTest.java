package com.mehdi.taskflow.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mehdi.taskflow.config.SecurityConfig;
import com.mehdi.taskflow.exception.ResourceNotFoundException;
import com.mehdi.taskflow.project.dto.ProjectRequest;
import com.mehdi.taskflow.security.JwtFilter;
import com.mehdi.taskflow.security.JwtService;
import com.mehdi.taskflow.security.UserDetailsServiceImpl;
import com.mehdi.taskflow.user.User;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

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

    private Project project;
    private ProjectRequest projectRequest;

    @BeforeEach
    void setUp() throws Exception {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("mehdi");

        project = new Project();
        project.setId(1L);
        project.setName("Mon projet");
        project.setDescription("Description");
        project.setOwner(owner);
        project.setCreatedAt(LocalDateTime.now());

        projectRequest = new ProjectRequest();
        projectRequest.setName("Mon projet");
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
                .andExpect(jsonPath("$[0].name").value("Mon projet"))
                .andExpect(jsonPath("$[0].id").value(1));
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
                .andExpect(jsonPath("$.name").value("Mon projet"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getProjectById_shouldReturn404_whenNotFound() throws Exception {
        // GIVEN
        when(projectService.getProjectById(999L))
                .thenThrow(new ResourceNotFoundException("Projet introuvable"));

        // WHEN & THEN
        mockMvc.perform(get("/api/projects/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Projet introuvable"));
    }

    @Test
    void getProjectById_shouldReturn403_whenNotOwner() throws Exception {
        // GIVEN
        when(projectService.getProjectById(1L))
                .thenThrow(new AccessDeniedException("Accès refusé"));

        // WHEN & THEN
        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Accès refusé"));
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
                .andExpect(jsonPath("$.name").value("Mon projet"))
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
                .andExpect(status().isBadRequest());
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
                .andExpect(jsonPath("$.name").value("Mon projet"));
    }

    @Test
    void updateProject_shouldReturn400_whenNameIsBlank() throws Exception {
        // GIVEN
        projectRequest.setName("");

        // WHEN & THEN
        mockMvc.perform(put("/api/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProject_shouldReturn403_whenNotOwner() throws Exception {
        // GIVEN
        when(projectService.updateProject(eq(1L), any(ProjectRequest.class)))
                .thenThrow(new AccessDeniedException("Accès refusé"));

        // WHEN & THEN
        mockMvc.perform(put("/api/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Accès refusé"));
    }

    @Test
    void updateProject_shouldReturn404_whenNotFound() throws Exception {
        // GIVEN
        when(projectService.updateProject(eq(999L), any(ProjectRequest.class)))
                .thenThrow(new ResourceNotFoundException("Projet introuvable"));

        // WHEN & THEN
        mockMvc.perform(put("/api/projects/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Projet introuvable"));
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
        doThrow(new AccessDeniedException("Accès refusé"))
                .when(projectService).deleteProject(1L);

        // WHEN & THEN
        mockMvc.perform(delete("/api/projects/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Accès refusé"));
    }

    @Test
    void deleteProject_shouldReturn404_whenNotFound() throws Exception {
        // GIVEN
        doThrow(new ResourceNotFoundException("Projet introuvable"))
                .when(projectService).deleteProject(1L);

        // WHEN & THEN
        mockMvc.perform(delete("/api/projects/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Projet introuvable"));
    }
}
