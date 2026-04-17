package com.mehdi.taskflow.project;

import com.mehdi.taskflow.config.MessageService;
import com.mehdi.taskflow.exception.ResourceNotFoundException;
import com.mehdi.taskflow.project.dto.ProjectRequest;
import com.mehdi.taskflow.security.SecurityUtils;
import com.mehdi.taskflow.user.User;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private ProjectService projectService;

    private User currentUser;
    private Project project;
    private ProjectRequest createRequest;
    private ProjectRequest updateRequest;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("mehdi");
        currentUser.setEmail("mehdi@test.com");

        project = new Project();
        project.setId(1L);
        project.setName("Existing project");
        project.setDescription("Existing description");
        project.setOwner(currentUser);

        createRequest = new ProjectRequest();
        createRequest.setName("New project");
        createRequest.setDescription("New description");

        updateRequest = new ProjectRequest();
        updateRequest.setName("Updated project");
        updateRequest.setDescription("Updated description");

        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
    }

    @Test
    void getMyProjects_shouldReturnProjectsOfCurrentUser() {
        // GIVEN
        when(projectRepository.findByOwnerId(1L)).thenReturn(List.of(project));

        // WHEN
        List<Project> projects = projectService.getMyProjects();

        // THEN
        assertEquals(1, projects.size());
        assertEquals("Existing project", projects.getFirst().getName());
        assertEquals("Existing description", projects.getFirst().getDescription());
        verify(securityUtils).getCurrentUser();
        verify(projectRepository).findByOwnerId(1L);
    }

    @Test
    void getMyProjects_shouldReturnEmptyList_whenNoProjects() {
        // GIVEN
        when(projectRepository.findByOwnerId(1L)).thenReturn(List.of());

        // WHEN
        List<Project> projects = projectService.getMyProjects();

        // THEN
        assertNotNull(projects);
        assertTrue(projects.isEmpty());
        verify(securityUtils).getCurrentUser();
        verify(projectRepository).findByOwnerId(1L);
    }

    @Test
    void getProjectById_shouldReturn_whenOwner() {
        // GIVEN
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        // WHEN
        Project result = projectService.getProjectById(1L);

        // THEN
        assertNotNull(result);
        assertEquals(project, result);
        verify(securityUtils).getCurrentUser();
        verify(projectRepository).findById(1L);
        verify(messageService, never()).get(anyString());
    }

    @Test
    void getProjectById_shouldThrow_whenNotFound() {
        // GIVEN
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageService.get("error.project.not.found")).thenReturn("Project not found");

        // WHEN
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> projectService.getProjectById(999L));

        assertEquals("Project not found", ex.getMessage());
        verify(securityUtils).getCurrentUser();
        verify(projectRepository).findById(999L);
        verify(messageService).get("error.project.not.found");
        verify(messageService, never()).get("error.access.denied");
    }

    @Test
    void getProjectById_shouldThrow_whenNotOwner() {
        // GIVEN
        User otherUser = new User();
        otherUser.setId(2L);
        project.setOwner(otherUser);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(messageService.get("error.access.denied")).thenReturn("Access denied");

        // WHEN
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> projectService.getProjectById(1L));

        // THEN
        assertEquals("Access denied", ex.getMessage());
        verify(securityUtils).getCurrentUser();
        verify(projectRepository).findById(1L);
        verify(messageService, never()).get("error.project.not.found");
        verify(messageService).get("error.access.denied");
    }

    @Test
    void createProject_shouldCreateAndReturnProject() {
        // GIVEN
        when(projectRepository.save(any(Project.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        Project result = projectService.createProject(createRequest);

        // THEN
        assertNotNull(result);
        assertEquals("New project", result.getName());
        assertEquals("New description", result.getDescription());
        assertEquals(currentUser, result.getOwner());
        assertNull(result.getId());
        assertNull(result.getCreatedAt());
        verify(securityUtils).getCurrentUser();
        verify(projectRepository).save(argThat(p ->
                p.getId() == null
                        && p.getCreatedAt() == null
                        && p.getName().equals("New project")
                        && p.getDescription().equals("New description")
                        && p.getOwner().equals(currentUser)
        ));
    }

    @Test
    void updateProject_shouldUpdateAndReturnProject() {
        // GIVEN
        when(projectRepository.existsByIdAndOwnerId(1L, 1L)).thenReturn(true);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        // WHEN
        Project result = projectService.updateProject(1L, updateRequest);

        // THEN
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Updated project", result.getName());
        assertEquals("Updated description", result.getDescription());
        assertEquals(currentUser, result.getOwner());
        verify(securityUtils).getCurrentUser();
        verify(projectRepository).existsByIdAndOwnerId(1L, 1L);
        verify(projectRepository).findById(1L);
        verify(projectRepository).save(argThat(p ->
                p.getId().equals(1L)
                        && p.getName().equals("Updated project")
                        && p.getDescription().equals("Updated description")
                        && p.getOwner().equals(currentUser)
                        && p.getCreatedAt() == null
        ));
    }

    @Test
    void updateProject_shouldThrow_whenNotOwner() {
        // GIVEN
        when(projectRepository.existsByIdAndOwnerId(1L, 1L)).thenReturn(false);
        when(messageService.get("error.access.denied")).thenReturn("Access denied");

        // WHEN
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> projectService.updateProject(1L, updateRequest));

        // THEN
        assertEquals("Access denied", ex.getMessage());
        verify(securityUtils).getCurrentUser();
        verify(projectRepository).existsByIdAndOwnerId(1L, 1L);
        verify(messageService).get("error.access.denied");
        verify(projectRepository, never()).findById(any());
        verify(messageService, never()).get("error.project.not.found");
        verify(projectRepository, never()).save(any());
    }

    @Test
    void updateProject_shouldThrow_whenProjectNotFound() {
        // GIVEN
        when(projectRepository.existsByIdAndOwnerId(1L, 1L)).thenReturn(true);
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());
        when(messageService.get("error.project.not.found")).thenReturn("Project not found");

        // WHEN
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> projectService.updateProject(1L, updateRequest));

        // THEN
        assertEquals("Project not found", ex.getMessage());
        verify(securityUtils).getCurrentUser();
        verify(projectRepository).existsByIdAndOwnerId(1L, 1L);
        verify(messageService, never()).get("error.access.denied");
        verify(projectRepository).findById(1L);
        verify(messageService).get("error.project.not.found");
        verify(projectRepository, never()).save(any());
    }

    @Test
    void deleteProject_shouldDelete_whenOwner() {
        // GIVEN
        when(projectRepository.existsByIdAndOwnerId(1L, 1L)).thenReturn(true);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        // WHEN
        projectService.deleteProject(1L);

        // THEN
        verify(securityUtils).getCurrentUser();
        verify(projectRepository).existsByIdAndOwnerId(1L, 1L);
        verify(messageService, never()).get("error.access.denied");
        verify(projectRepository).findById(1L);
        verify(messageService, never()).get("error.project.not.found");
        verify(projectRepository, times(1)).delete(project);
    }

    @Test
    void deleteProject_shouldThrow_whenNotOwner() {
        // GIVEN
        when(projectRepository.existsByIdAndOwnerId(1L, 1L)).thenReturn(false);
        when(messageService.get("error.access.denied")).thenReturn("Access denied");

        // WHEN
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> projectService.deleteProject(1L));

        // THEN
        assertEquals("Access denied", ex.getMessage());
        verify(securityUtils).getCurrentUser();
        verify(projectRepository).existsByIdAndOwnerId(1L, 1L);
        verify(messageService).get("error.access.denied");
        verify(projectRepository, never()).findById(any());
        verify(messageService, never()).get("error.project.not.found");
        verify(projectRepository, never()).delete(any());
    }

    @Test
    void deleteProject_shouldThrow_whenProjectNotFound() {
        // GIVEN
        when(projectRepository.existsByIdAndOwnerId(1L, 1L)).thenReturn(true);
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());
        when(messageService.get("error.project.not.found")).thenReturn("Project not found");

        // WHEN
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> projectService.deleteProject(1L));

        // THEN
        assertEquals("Project not found", ex.getMessage());
        verify(securityUtils).getCurrentUser();
        verify(projectRepository).existsByIdAndOwnerId(1L, 1L);
        verify(messageService, never()).get("error.access.denied");
        verify(projectRepository).findById(1L);
        verify(messageService).get("error.project.not.found");
        verify(projectRepository, never()).delete(any());
    }
}