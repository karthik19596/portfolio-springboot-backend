package com.premkarthik.portfolio.service;

import com.premkarthik.portfolio.dto.AdminTaskResponse;
import com.premkarthik.portfolio.dto.AdminUserResponse;
import com.premkarthik.portfolio.dto.AdminUserUpdateRequest;
import com.premkarthik.portfolio.dto.AdminUserCreateRequest;
import com.premkarthik.portfolio.dto.RoleUpdateRequest;
import com.premkarthik.portfolio.dto.TaskRequest;
import com.premkarthik.portfolio.exception.ResourceNotFoundException;
import com.premkarthik.portfolio.model.Task;
import com.premkarthik.portfolio.model.User;
import com.premkarthik.portfolio.repository.TaskRepository;
import com.premkarthik.portfolio.repository.UserRepository;
import com.premkarthik.portfolio.repository.PasswordResetTokenRepository;
import com.premkarthik.portfolio.repository.RefreshTokenRepository;
import com.premkarthik.portfolio.security.UserDetailsImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final AuditLogService auditLogService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserRepository userRepository,
                        TaskRepository taskRepository,
                        AuditLogService auditLogService,
                        RefreshTokenRepository refreshTokenRepository,
                        PasswordResetTokenRepository passwordResetTokenRepository,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.auditLogService = auditLogService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AdminUserResponse> getUsers(Authentication authentication) {
        boolean superAdmin = isSuperAdmin(authentication);
        return userRepository.findAll().stream()
                .filter(user -> superAdmin
                        || user.getRole().equals("USER"))
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional
    public AdminUserResponse createUser(AdminUserCreateRequest request, Authentication authentication) {
        String role = request.getRole().trim().toUpperCase();
        if (!isSupportedRole(role)) {
            throw new IllegalArgumentException("Role must be USER, ADMIN, or SUPER_ADMIN");
        }
        if (role.equals("ADMIN") || role.equals("SUPER_ADMIN")) {
            if (!isSuperAdmin(authentication)) {
                throw new IllegalArgumentException("Only a SUPER_ADMIN can create administrator accounts");
            }
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        return AdminUserResponse.from(userRepository.save(user));
    }

    @Transactional
    public AdminUserResponse updateRole(Long userId, RoleUpdateRequest request,
                                        Authentication authentication) {
        String role = request.getRole().trim().toUpperCase();
        if (!isSupportedRole(role)) {
            throw new IllegalArgumentException("Role must be USER, ADMIN, or SUPER_ADMIN");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        ensureAdminManagementAllowed(user, authentication);
        if (role.equals("SUPER_ADMIN") && !isSuperAdmin(authentication)) {
            throw new IllegalArgumentException("Only a SUPER_ADMIN can assign the SUPER_ADMIN role");
        }
        user.setRole(role);
        return AdminUserResponse.from(userRepository.save(user));
    }

    @Transactional
    public AdminUserResponse updateUser(Long userId, AdminUserUpdateRequest request,
                                        Authentication authentication) {
        String role = request.getRole().trim().toUpperCase();
        if (!isSupportedRole(role)) {
            throw new IllegalArgumentException("Role must be USER, ADMIN, or SUPER_ADMIN");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        ensureAdminManagementAllowed(user, authentication);
        if (role.equals("SUPER_ADMIN") && !isSuperAdmin(authentication)) {
            throw new IllegalArgumentException("Only a SUPER_ADMIN can assign the SUPER_ADMIN role");
        }
        if (userRepository.existsByUsernameAndIdNot(request.getUsername(), userId)) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.existsByEmailAndIdNot(request.getEmail(), userId)) {
            throw new IllegalArgumentException("Email already registered");
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setRole(role);
        return AdminUserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long userId, Authentication authentication) {
        User currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (currentUser.getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        ensureAdminManagementAllowed(user, authentication);
        taskRepository.deleteByUser_Id(userId);
        refreshTokenRepository.deleteByUser_Id(userId);
        passwordResetTokenRepository.deleteByUser_Id(userId);
        userRepository.delete(user);
    }

    private void ensureAdminManagementAllowed(User target, Authentication authentication) {
        if ((target.getRole().equals("ADMIN") || target.getRole().equals("SUPER_ADMIN"))
                && !isSuperAdmin(authentication)) {
            throw new IllegalArgumentException("Only a SUPER_ADMIN can manage administrator accounts");
        }
    }

    private boolean isSuperAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }

    private boolean isSupportedRole(String role) {
        return role.equals("USER") || role.equals("ADMIN") || role.equals("SUPER_ADMIN");
    }

    @Transactional(readOnly = true)
    public Page<AdminTaskResponse> getTasks(Pageable pageable) {
        return taskRepository.findAll(pageable).map(AdminTaskResponse::from);
    }

    @Transactional
    public AdminTaskResponse createTask(TaskRequest request, Authentication authentication) {
        User creator = currentUser(authentication);
        User assignee = getAssignee(request.getAssignedUserId());
        validateAssignment(creator, assignee);

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setUser(assignee);
        Task saved = taskRepository.save(task);
        auditLogService.log("CREATE", "Task", saved.getId(), creator.getUsername(),
                "Assigned task '" + saved.getTitle() + "' to " + assignee.getUsername());
        return AdminTaskResponse.from(saved);
    }

    @Transactional
    public AdminTaskResponse updateTask(Long taskId, TaskRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        Task saved = taskRepository.save(task);
        auditLogService.log("UPDATE", "Task", saved.getId(), task.getUser().getUsername(),
                "Admin updated task: " + saved.getTitle());
        return AdminTaskResponse.from(saved);
    }

    private User getAssignee(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("An assignee is required");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
    }

    private void validateAssignment(User creator, User assignee) {
        if (creator.getRole().equals("ADMIN") && !assignee.getRole().equals("USER")) {
            throw new IllegalArgumentException("ADMIN users can assign tasks only to USER accounts");
        }
        if (creator.getRole().equals("SUPER_ADMIN")
                && !(assignee.getRole().equals("USER") || assignee.getRole().equals("ADMIN"))) {
            throw new IllegalArgumentException("SUPER_ADMIN users can assign tasks to USER or ADMIN accounts");
        }
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public void deleteTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
        taskRepository.delete(task);
        auditLogService.log("DELETE", "Task", taskId, task.getUser().getUsername(),
                "Admin deleted task: " + task.getTitle());
    }
}
