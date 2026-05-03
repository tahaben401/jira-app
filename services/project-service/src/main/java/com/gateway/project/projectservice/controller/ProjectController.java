package com.gateway.project.projectservice.controller;

import com.gateway.project.projectservice.dto.ProjectDtos.*;
import com.gateway.project.projectservice.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    // X-User-Id is injected by the API Gateway after JWT validation

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> create(
            @Valid @RequestBody CreateProjectRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Project created", projectService.createProject(request, userId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getMyProjects(
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(ApiResponse.ok(projectService.getMyProjects(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectDetailResponse>> getById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(ApiResponse.ok(projectService.getProjectById(id, userId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(ApiResponse.ok("Project updated", projectService.updateProject(id, request, userId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {

        projectService.deleteProject(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Project deleted", null));
    }

    // ── Member management ─────────────────────────────────────────────────────

    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<MemberResponse>> inviteMember(
            @PathVariable Long id,
            @Valid @RequestBody InviteMemberRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Member invited", projectService.inviteMember(id, request, userId)));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @RequestHeader("X-User-Id") Long userId) {

        projectService.removeMember(id, memberId, userId);
        return ResponseEntity.ok(ApiResponse.ok("Member removed", null));
    }

    @PutMapping("/{id}/members/{memberId}/role")
    public ResponseEntity<ApiResponse<MemberResponse>> updateMemberRole(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(ApiResponse.ok("Role updated",
                projectService.updateMemberRole(id, memberId, request, userId)));
    }
}
