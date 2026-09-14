package com.premkarthik.portfolio.controller;

import com.premkarthik.portfolio.dto.ApiResponse;
import com.premkarthik.portfolio.dto.AdminTaskResponse;
import com.premkarthik.portfolio.dto.AdminUserResponse;
import com.premkarthik.portfolio.dto.AdminUserUpdateRequest;
import com.premkarthik.portfolio.dto.AdminUserCreateRequest;
import com.premkarthik.portfolio.dto.RoleUpdateRequest;
import com.premkarthik.portfolio.dto.TaskRequest;
import com.premkarthik.portfolio.model.AuditLog;
import com.premkarthik.portfolio.service.AdminService;
import com.premkarthik.portfolio.service.AuditLogService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin", description = "Admin-only operations")
public class AdminController {

    private final AuditLogService auditLogService;
    private final AdminService adminService;

    public AdminController(AuditLogService auditLogService, AdminService adminService) {
        this.auditLogService = auditLogService;
        this.adminService = adminService;
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getAuditLogs() {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getTaskAuditLogs()));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> getUsers(
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getUsers(authentication)));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<AdminUserResponse>> createUser(
            @Valid @RequestBody AdminUserCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                "User created", adminService.createUser(request, authentication)));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateRole(
            @PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                "User role updated", adminService.updateRole(id, request, authentication)));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUser(
            @PathVariable Long id, @Valid @RequestBody AdminUserUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                "User updated", adminService.updateUser(id, request, authentication)));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long id, Authentication authentication) {
        adminService.deleteUser(id, authentication);
        return ResponseEntity.ok(ApiResponse.success("User deleted", null));
    }

    @GetMapping("/tasks")
    public ResponseEntity<ApiResponse<Page<AdminTaskResponse>>> getTasks(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getTasks(pageable)));
    }

    @PostMapping("/tasks")
    public ResponseEntity<ApiResponse<AdminTaskResponse>> createTask(
            @Valid @RequestBody TaskRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                "Task assigned", adminService.createTask(request, authentication)));
    }

    @PutMapping("/tasks/{id}")
    public ResponseEntity<ApiResponse<AdminTaskResponse>> updateTask(
            @PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Task updated", adminService.updateTask(id, request)));
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long id) {
        adminService.deleteTask(id);
        return ResponseEntity.ok(ApiResponse.success("Task deleted", null));
    }
}
