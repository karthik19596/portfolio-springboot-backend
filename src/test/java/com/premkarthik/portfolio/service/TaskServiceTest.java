package com.premkarthik.portfolio.service;

import com.premkarthik.portfolio.dto.TaskRequest;
import com.premkarthik.portfolio.exception.ResourceNotFoundException;
import com.premkarthik.portfolio.model.Task;
import com.premkarthik.portfolio.model.User;
import com.premkarthik.portfolio.repository.TaskRepository;
import com.premkarthik.portfolio.repository.UserRepository;
import com.premkarthik.portfolio.security.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TaskService taskService;

    private User user;
    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("demo");
        user.setEmail("demo@example.com");
        user.setPassword("encoded");
        user.setRole("USER");

        userDetails = UserDetailsImpl.build(user);
    }

    @Test
    void createTask_shouldReturnSavedTask() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findByUsername("demo")).thenReturn(Optional.of(user));

        TaskRequest request = new TaskRequest();
        request.setTitle("Test Task");
        request.setDescription("Description");
        request.setStatus("TODO");
        request.setPriority("HIGH");

        Task savedTask = new Task();
        savedTask.setId(1L);
        savedTask.setTitle(request.getTitle());
        savedTask.setDescription(request.getDescription());
        savedTask.setStatus(request.getStatus());
        savedTask.setPriority(request.getPriority());
        savedTask.setUser(user);

        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);
        when(auditLogService.log(anyString(), anyString(), anyLong(), anyString(), anyString()))
                .thenReturn(null);

        Task result = taskService.createTask(request, authentication);

        assertNotNull(result);
        assertEquals("Test Task", result.getTitle());
        assertEquals("demo", result.getUser().getUsername());
        verify(taskRepository, times(1)).save(any(Task.class));
        verify(auditLogService, times(1)).log(anyString(), anyString(), anyLong(), anyString(), anyString());
    }

    @Test
    void getTask_whenTaskExists_shouldReturnTask() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findByUsername("demo")).thenReturn(Optional.of(user));

        Task task = new Task();
        task.setId(1L);
        task.setTitle("Existing Task");
        task.setUser(user);

        when(taskRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(task));

        Task result = taskService.getTask(1L, authentication);

        assertNotNull(result);
        assertEquals("Existing Task", result.getTitle());
    }

    @Test
    void getTask_whenTaskNotFound_shouldThrowException() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findByUsername("demo")).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.getTask(1L, authentication));
    }

    @Test
    void deleteTask_shouldDeleteAndLog() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findByUsername("demo")).thenReturn(Optional.of(user));

        Task task = new Task();
        task.setId(1L);
        task.setTitle("To Delete");
        task.setUser(user);

        when(taskRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(task));
        doNothing().when(taskRepository).delete(task);
        when(auditLogService.log(anyString(), anyString(), anyLong(), anyString(), anyString()))
                .thenReturn(null);

        taskService.deleteTask(1L, authentication);

        verify(taskRepository, times(1)).delete(task);
        verify(auditLogService, times(1)).log(anyString(), anyString(), anyLong(), anyString(), anyString());
    }
}
