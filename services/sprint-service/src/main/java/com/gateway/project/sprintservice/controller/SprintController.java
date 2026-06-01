package com.gateway.project.sprintservice.controller;

import com.gateway.project.sprintservice.dto.SprintDtos.*;
import com.gateway.project.sprintservice.service.SprintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sprints")
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;

    @PostMapping
    public ResponseEntity<ApiResponse<SprintResponse>> create(
            @Valid @RequestBody CreateSprintRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        SprintResponse response = sprintService.createSprint(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Sprint created", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SprintResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(sprintService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SprintResponse>>> getByProject(
            @RequestParam Long projectId) {

        return ResponseEntity.ok(ApiResponse.ok(sprintService.getByProject(projectId)));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<SprintResponse>> getActive(
            @RequestParam Long projectId) {

        return ResponseEntity.ok(ApiResponse.ok(sprintService.getActiveByProject(projectId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SprintResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSprintRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(ApiResponse.ok(
                "Sprint updated", sprintService.updateSprint(id, request, userId)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<SprintResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(ApiResponse.ok(
                "Status updated", sprintService.updateStatus(id, request, userId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {

        sprintService.deleteSprint(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Sprint deleted", null));
    }
}
