package com.mehdi.taskflow.project;

import com.mehdi.taskflow.exception.ResourceNotFoundException;
import com.mehdi.taskflow.project.dto.ProjectRequest;
import com.mehdi.taskflow.user.User;
import com.mehdi.taskflow.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

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
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ProjectService projectService;

    private User currentUser;
    private Project project;
    private ProjectRequest projectRequest;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("mehdi");
        currentUser.setEmail("mehdi@test.com");

        project = new Project();
        project.setId(1L);
        project.setName("Mon projet");
        project.setOwner(currentUser);

        projectRequest = new ProjectRequest();
        projectRequest.setName("Nouveau nom");
        projectRequest.setDescription("Description");

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("mehdi");
        when(userRepository.findByUsername("mehdi")).thenReturn(Optional.of(currentUser));
    }

    @Test
    void getMyProjects_shouldReturnProjectsOfCurrentUser() {
        // GIVEN
        when(projectRepository.findByOwnerId(1L)).thenReturn(List.of(project));

        // WHEN
        List<Project> projects = projectService.getMyProjects();

        // THEN
        assertEquals(1, projects.size());
        assertEquals("Mon projet", projects.get(0).getName());
    }

    @Test
    void createProject_shouldCreateAndReturnProject() {
        // GIVEN
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        // WHEN
        Project result = projectService.createProject(projectRequest);

        // THEN
        assertNotNull(result);
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void updateProject_shouldThrow_whenNotOwner() {
        // GIVEN — accès refusé avant même de charger le projet
        when(projectRepository.existsByIdAndOwnerId(1L, 1L)).thenReturn(false);

        // WHEN & THEN
        assertThrows(AccessDeniedException.class,
                () -> projectService.updateProject(1L, projectRequest));

        // verify le projet n'est jamais chargé
        verify(projectRepository, never()).findById(any());
    }

    @Test
    void updateProject_shouldThrow_whenProjectNotFound() {
        // GIVEN — accès ok mais projet introuvable
        when(projectRepository.existsByIdAndOwnerId(1L, 1L)).thenReturn(true);
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(ResourceNotFoundException.class,
                () -> projectService.updateProject(1L, projectRequest));
    }

    @Test
    void updateProject_shouldUpdateAndReturnProject() {
        // GIVEN
        when(projectRepository.existsByIdAndOwnerId(1L, 1L)).thenReturn(true);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        // WHEN
        Project result = projectService.updateProject(1L, projectRequest);

        // THEN
        assertNotNull(result);
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void deleteProject_shouldThrow_whenNotOwner() {
        // GIVEN
        when(projectRepository.existsByIdAndOwnerId(1L, 1L)).thenReturn(false);

        // WHEN & THEN
        assertThrows(AccessDeniedException.class,
                () -> projectService.deleteProject(1L));

        verify(projectRepository, never()).findById(any());
        verify(projectRepository, never()).delete(any());
    }

    @Test
    void deleteProject_shouldThrow_whenProjectNotFound() {
        // GIVEN
        when(projectRepository.existsByIdAndOwnerId(1L, 1L)).thenReturn(true);
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(ResourceNotFoundException.class,
                () -> projectService.deleteProject(1L));

        verify(projectRepository, never()).delete(any());
    }

    @Test
    void deleteProject_shouldDelete_whenOwner() {
        // GIVEN
        when(projectRepository.existsByIdAndOwnerId(1L, 1L)).thenReturn(true);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        // WHEN
        projectService.deleteProject(1L);

        // THEN
        verify(projectRepository, times(1)).delete(project);
    }

    @Test
    void getProjectById_shouldThrow_whenNotOwner() {
        // GIVEN
        User otherUser = new User();
        otherUser.setId(2L);
        project.setOwner(otherUser);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        // WHEN & THEN
        assertThrows(AccessDeniedException.class,
                () -> projectService.getProjectById(1L));
    }

    @Test
    void getProjectById_shouldThrow_whenNotFound() {
        // GIVEN
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(ResourceNotFoundException.class,
                () -> projectService.getProjectById(999L));
    }

    @Test
    void getProjectById_shouldReturn_whenOwner() {
        // GIVEN
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        // WHEN
        Project result = projectService.getProjectById(1L);

        // THEN
        assertNotNull(result);
        assertEquals("Mon projet", result.getName());
    }
}