package com.premkarthik.portfolio.controller;

import com.premkarthik.portfolio.dto.ApiResponse;
import com.premkarthik.portfolio.dto.TaskRequest;
import com.premkarthik.portfolio.dto.TaskStatsResponse;
import com.premkarthik.portfolio.model.Task;
import com.premkarthik.portfolio.service.TaskService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tasks", description = "CRUD operations for tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Task>> createTask(@Valid @RequestBody TaskRequest request,
                                                          Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Task created", taskService.createTask(request, authentication)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Task>>> getTasks(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                                                            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTasks(authentication, pageable)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<TaskStatsResponse>> getStats(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getStats(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Task>> getTask(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTask(id, authentication)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Task>> updateTask(@PathVariable Long id,
                                                          @Valid @RequestBody TaskRequest request,
                                                          Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Task updated", taskService.updateTask(id, request, authentication)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long id, Authentication authentication) {
        taskService.deleteTask(id, authentication);
        return ResponseEntity.ok(ApiResponse.success("Task deleted", null));
    }
}
