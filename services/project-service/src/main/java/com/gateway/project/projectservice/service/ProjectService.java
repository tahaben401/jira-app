package com.gateway.project.projectservice.service;

import com.gateway.project.projectservice.client.AuthServiceClient;
import com.gateway.project.projectservice.dto.ProjectDtos.*;
import com.gateway.project.projectservice.entity.Project;
import com.gateway.project.projectservice.entity.ProjectMember;
import com.gateway.project.projectservice.exception.ProjectException;
import com.gateway.project.projectservice.repository.ProjectMemberRepository;
import com.gateway.project.projectservice.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository       projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final AuthServiceClient       authServiceClient;

    // ── Create project ────────────────────────────────────────────────────────

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, Long ownerId) {
        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(ownerId)
                .status(Project.Status.ACTIVE)
                .build();

        project = projectRepository.save(project);
        log.info("Project created: {} by user {}", project.getId(), ownerId);
        return toResponse(project);
    }

    // ── Get project by ID ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ProjectDetailResponse getProjectById(Long projectId, Long requestingUserId) {
        Project project = findProject(projectId);
        assertMemberOrOwner(project, requestingUserId);

        List<ProjectMember> members = memberRepository.findByProjectId(projectId);
        List<MemberResponse> memberResponses = enrichMembers(members);

        return toDetailResponse(project, memberResponses);
    }

    // ── Get all projects for a user ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProjectResponse> getMyProjects(Long userId) {
        return projectRepository.findAllByUserId(userId)
                .stream().map(this::toResponse).toList();
    }

    // ── Update project ────────────────────────────────────────────────────────

    @Transactional
    public ProjectResponse updateProject(Long projectId, UpdateProjectRequest request, Long requestingUserId) {
        Project project = findProject(projectId);
        assertOwner(project, requestingUserId);

        if (request.getName() != null)        project.setName(request.getName());
        if (request.getDescription() != null) project.setDescription(request.getDescription());
        if (request.getStatus() != null)      project.setStatus(request.getStatus());

        return toResponse(projectRepository.save(project));
    }

    // ── Delete project ────────────────────────────────────────────────────────

    @Transactional
    public void deleteProject(Long projectId, Long requestingUserId) {
        Project project = findProject(projectId);
        assertOwner(project, requestingUserId);
        projectRepository.delete(project);
        log.info("Project {} deleted by user {}", projectId, requestingUserId);
    }

    // ── Invite member ─────────────────────────────────────────────────────────

    @Transactional
    public MemberResponse inviteMember(Long projectId, InviteMemberRequest request, Long requestingUserId) {
        Project project = findProject(projectId);
        assertOwnerOrScrumMaster(project, requestingUserId);

        if (memberRepository.existsByProjectIdAndUserId(projectId, request.getUserId())) {
            throw new ProjectException.ConflictException("User is already a member of this project");
        }

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .userId(request.getUserId())
                .role(request.getRole())
                .build();

        member = memberRepository.save(member);
        log.info("User {} invited to project {} as {}", request.getUserId(), projectId, request.getRole());

        // Enrich with user info from auth-service
        List<MemberResponse> enriched = enrichMembers(List.of(member));
        return enriched.get(0);
    }

    // ── Remove member ─────────────────────────────────────────────────────────

    @Transactional
    public void removeMember(Long projectId, Long userId, Long requestingUserId) {
        Project project = findProject(projectId);
        assertOwnerOrScrumMaster(project, requestingUserId);

        if (!memberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new ProjectException.NotFoundException("Member", userId);
        }

        memberRepository.deleteByProjectIdAndUserId(projectId, userId);
        log.info("User {} removed from project {}", userId, projectId);
    }

    // ── Update member role ────────────────────────────────────────────────────

    @Transactional
    public MemberResponse updateMemberRole(Long projectId, Long userId, UpdateMemberRoleRequest request, Long requestingUserId) {
        Project project = findProject(projectId);
        assertOwner(project, requestingUserId);

        ProjectMember member = memberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ProjectException.NotFoundException("Member", userId));

        member.setRole(request.getRole());
        member = memberRepository.save(member);

        List<MemberResponse> enriched = enrichMembers(List.of(member));
        return enriched.get(0);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Project findProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ProjectException.NotFoundException("Project", id));
    }

    private void assertOwner(Project project, Long userId) {
        if (!project.getOwnerId().equals(userId)) {
            throw new ProjectException.UnauthorizedException("Only the project owner can perform this action");
        }
    }

    private void assertMemberOrOwner(Project project, Long userId) {
        boolean isOwner = project.getOwnerId().equals(userId);
        boolean isMember = memberRepository.existsByProjectIdAndUserId(project.getId(), userId);
        if (!isOwner && !isMember) {
            throw new ProjectException.UnauthorizedException("You are not a member of this project");
        }
    }

    private void assertOwnerOrScrumMaster(Project project, Long userId) {
        if (project.getOwnerId().equals(userId)) return;
        memberRepository.findByProjectIdAndUserId(project.getId(), userId)
                .filter(m -> m.getRole() == ProjectMember.Role.SCRUM_MASTER)
                .orElseThrow(() -> new ProjectException.UnauthorizedException(
                        "Only the owner or scrum master can perform this action"));
    }

    private List<MemberResponse> enrichMembers(List<ProjectMember> members) {
        if (members.isEmpty()) return List.of();

        List<Long> userIds = members.stream().map(ProjectMember::getUserId).toList();

        try {
            var apiResp = authServiceClient.getUsersByIds(userIds);
            Map<Long, AuthServiceClient.UserSummary> userMap = apiResp.getData()
                    .stream().collect(Collectors.toMap(AuthServiceClient.UserSummary::getId, u -> u));

            return members.stream().map(m -> {
                var user = userMap.get(m.getUserId());
                return MemberResponse.builder()
                        .id(m.getId())
                        .userId(m.getUserId())
                        .fullName(user != null ? user.getFullName() : "Unknown")
                        .email(user != null ? user.getEmail() : "")
                        .role(m.getRole().name())
                        .joinedAt(m.getJoinedAt())
                        .build();
            }).toList();
        } catch (Exception e) {
            log.warn("Could not fetch user details from auth-service: {}", e.getMessage());
            // Graceful degradation — return members without user info
            return members.stream().map(m -> MemberResponse.builder()
                    .id(m.getId())
                    .userId(m.getUserId())
                    .role(m.getRole().name())
                    .joinedAt(m.getJoinedAt())
                    .build()).toList();
        }
    }

    private ProjectResponse toResponse(Project p) {
        return ProjectResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .ownerId(p.getOwnerId())
                .status(p.getStatus().name())
                .memberCount(p.getMembers().size())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private ProjectDetailResponse toDetailResponse(Project p, List<MemberResponse> members) {
        return ProjectDetailResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .ownerId(p.getOwnerId())
                .status(p.getStatus().name())
                .members(members)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
