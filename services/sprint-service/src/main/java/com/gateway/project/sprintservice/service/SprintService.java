package com.gateway.project.sprintservice.service;

import com.gateway.project.sprintservice.client.AuthServiceClient;
import com.gateway.project.sprintservice.dto.SprintDtos.*;
import com.gateway.project.sprintservice.entity.Sprint;
import com.gateway.project.sprintservice.exception.SprintException;
import com.gateway.project.sprintservice.repository.SprintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SprintService {

    private final SprintRepository sprintRepository;
    private final AuthServiceClient authServiceClient;

    @Transactional
    public SprintResponse createSprint(CreateSprintRequest request, Long userId) {
        validateDates(request.getStartDate(), request.getEndDate());

        Sprint sprint = Sprint.builder()
                .projectId(request.getProjectId())
                .name(request.getName())
                .goal(request.getGoal())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .createdBy(userId)
                .status(Sprint.Status.PLANNED)
                .build();

        sprint = sprintRepository.save(sprint);
        log.info("Sprint {} created in project {} by user {}", sprint.getId(), sprint.getProjectId(), userId);
        return enrich(sprint);
    }

    @Transactional(readOnly = true)
    public SprintResponse getById(Long sprintId) {
        return enrich(findSprint(sprintId));
    }

    @Transactional(readOnly = true)
    public List<SprintResponse> getByProject(Long projectId) {
        return enrichAll(sprintRepository.findByProjectId(projectId));
    }

    @Transactional(readOnly = true)
    public SprintResponse getActiveByProject(Long projectId) {
        Sprint sprint = sprintRepository.findByProjectIdAndStatus(projectId, Sprint.Status.ACTIVE)
                .orElseThrow(() -> new SprintException.NotFoundException("Active sprint", projectId));
        return enrich(sprint);
    }

    @Transactional
    public SprintResponse updateSprint(Long sprintId, UpdateSprintRequest request, Long userId) {
        Sprint sprint = findSprint(sprintId);
        assertCreator(sprint, userId);

        LocalDate nextStart = request.getStartDate() != null ? request.getStartDate() : sprint.getStartDate();
        LocalDate nextEnd = request.getEndDate() != null ? request.getEndDate() : sprint.getEndDate();
        validateDates(nextStart, nextEnd);

        if (request.getName() != null)      sprint.setName(request.getName());
        if (request.getGoal() != null)      sprint.setGoal(request.getGoal());
        if (request.getStartDate() != null) sprint.setStartDate(request.getStartDate());
        if (request.getEndDate() != null)   sprint.setEndDate(request.getEndDate());

        return enrich(sprintRepository.save(sprint));
    }

    @Transactional
    public SprintResponse updateStatus(Long sprintId, UpdateStatusRequest request, Long userId) {
        Sprint sprint = findSprint(sprintId);
        assertCreator(sprint, userId);

        if (request.getStatus() == Sprint.Status.ACTIVE) {
            sprintRepository.findByProjectIdAndStatus(sprint.getProjectId(), Sprint.Status.ACTIVE)
                    .filter(existing -> !existing.getId().equals(sprint.getId()))
                    .ifPresent(existing -> {
                        throw new SprintException.ConflictException(
                                "Another active sprint already exists for this project");
                    });
        }

        sprint.setStatus(request.getStatus());
        return enrich(sprintRepository.save(sprint));
    }

    @Transactional
    public void deleteSprint(Long sprintId, Long userId) {
        Sprint sprint = findSprint(sprintId);
        assertCreator(sprint, userId);
        sprintRepository.delete(sprint);
        log.info("Sprint {} deleted by user {}", sprintId, userId);
    }

    private Sprint findSprint(Long id) {
        return sprintRepository.findById(id)
                .orElseThrow(() -> new SprintException.NotFoundException("Sprint", id));
    }

    private void assertCreator(Sprint sprint, Long userId) {
        if (!sprint.getCreatedBy().equals(userId)) {
            throw new SprintException.UnauthorizedException("Only the sprint creator can perform this action");
        }
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new SprintException.BadRequestException("End date cannot be before start date");
        }
    }

    private SprintResponse enrich(Sprint sprint) {
        return enrichAll(List.of(sprint)).get(0);
    }

    private List<SprintResponse> enrichAll(List<Sprint> sprints) {
        if (sprints.isEmpty()) return List.of();

        List<Long> userIds = sprints.stream().map(Sprint::getCreatedBy).distinct().toList();
        Map<Long, String> userNames = fetchUserNames(userIds);

        return sprints.stream().map(s -> SprintResponse.builder()
                .id(s.getId())
                .projectId(s.getProjectId())
                .name(s.getName())
                .goal(s.getGoal())
                .status(s.getStatus().name())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .createdBy(s.getCreatedBy())
                .createdByName(userNames.getOrDefault(s.getCreatedBy(), "Unknown"))
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build()).toList();
    }

    private Map<Long, String> fetchUserNames(List<Long> userIds) {
        try {
            var resp = authServiceClient.getUsersByIds(userIds);
            return resp.getData().stream()
                    .collect(Collectors.toMap(
                            AuthServiceClient.UserSummary::getId,
                            AuthServiceClient.UserSummary::getFullName
                    ));
        } catch (Exception e) {
            log.warn("Could not fetch user info from auth-service: {}", e.getMessage());
            return Map.of();
        }
    }
}
