package com.gateway.project.taskservice.controller;

import com.gateway.project.taskservice.dto.TaskDtos.*;
import com.gateway.project.taskservice.entity.Task;
import com.gateway.project.taskservice.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> create(
            @Valid @RequestBody CreateTaskRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Task created", taskService.createTask(request, userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.getById(id)));
    }

    // GET /api/v1/tasks?projectId=1&status=TODO&type=BUG
    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getByProject(
            @RequestParam Long projectId,
            @RequestParam(required = false) Task.Status status,
            @RequestParam(required = false) Task.Type type) {

        return ResponseEntity.ok(ApiResponse.ok(taskService.getByProject(projectId, status, type)));
    }

    @GetMapping("/backlog")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getBacklog(
            @RequestParam Long projectId) {

        return ResponseEntity.ok(ApiResponse.ok(taskService.getBacklog(projectId)));
    }

    @GetMapping("/sprint/{sprintId}")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getBySprint(
            @PathVariable Long sprintId) {

        return ResponseEntity.ok(ApiResponse.ok(taskService.getBySprint(sprintId)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getMyTasks(
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(ApiResponse.ok(taskService.getMyTasks(userId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(ApiResponse.ok("Task updated", taskService.updateTask(id, request, userId)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TaskResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(ApiResponse.ok("Status updated", taskService.updateStatus(id, request, userId)));
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<TaskResponse>> assign(
            @PathVariable Long id,
            @RequestBody AssignTaskRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(ApiResponse.ok("Task assigned", taskService.assignTask(id, request, userId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {

        taskService.deleteTask(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Task deleted", null));
    }
}
