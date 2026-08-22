package com.premkarthik.portfolio.service;

import com.premkarthik.portfolio.dto.TaskRequest;
import com.premkarthik.portfolio.exception.ResourceNotFoundException;
import com.premkarthik.portfolio.model.Task;
import com.premkarthik.portfolio.model.User;
import com.premkarthik.portfolio.repository.TaskRepository;
import com.premkarthik.portfolio.repository.UserRepository;
import com.premkarthik.portfolio.security.UserDetailsImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public TaskService(TaskRepository taskRepository,
                       UserRepository userRepository,
                       AuditLogService auditLogService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Task createTask(TaskRequest request, Authentication authentication) {
        User user = getCurrentUser(authentication);

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setUser(user);

        Task saved = taskRepository.save(task);
        auditLogService.log("CREATE", "Task", saved.getId(), user.getUsername(),
                "Created task: " + saved.getTitle());
        return saved;
    }

    public Page<Task> getTasks(Authentication authentication, Pageable pageable) {
        User user = getCurrentUser(authentication);
        return taskRepository.findByUserId(user.getId(), pageable);
    }

    public Task getTask(Long id, Authentication authentication) {
        User user = getCurrentUser(authentication);
        return taskRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    @Transactional
    public Task updateTask(Long id, TaskRequest request, Authentication authentication) {
        Task task = getTask(id, authentication);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());

        Task updated = taskRepository.save(task);
        auditLogService.log("UPDATE", "Task", updated.getId(), task.getUser().getUsername(),
                "Updated task status to " + updated.getStatus());
        return updated;
    }

    @Transactional
    public void deleteTask(Long id, Authentication authentication) {
        Task task = getTask(id, authentication);
        taskRepository.delete(task);
        auditLogService.log("DELETE", "Task", id, task.getUser().getUsername(),
                "Deleted task: " + task.getTitle());
    }

    private User getCurrentUser(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
