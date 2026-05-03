package com.gateway.project.taskservice.service;

import com.gateway.project.taskservice.client.AuthServiceClient;
import com.gateway.project.taskservice.dto.TaskDtos.*;
import com.gateway.project.taskservice.entity.Task;
import com.gateway.project.taskservice.exception.TaskException;
import com.gateway.project.taskservice.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository   taskRepository;
    private final AuthServiceClient authServiceClient;

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request, Long reporterId) {
        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .projectId(request.getProjectId())
                .reporterId(reporterId)
                .assigneeId(request.getAssigneeId())
                .type(request.getType() != null ? request.getType() : Task.Type.TASK)
                .status(Task.Status.TODO)
                .priority(request.getPriority() != null ? request.getPriority() : Task.Priority.MEDIUM)
                .storyPoints(request.getStoryPoints())
                .dueDate(request.getDueDate())
                .build();

        task = taskRepository.save(task);
        log.info("Task {} created in project {} by user {}", task.getId(), task.getProjectId(), reporterId);
        return enrich(task);
    }

    // ── Get by ID ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TaskResponse getById(Long taskId) {
        return enrich(findTask(taskId));
    }

    // ── Get by project ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TaskResponse> getByProject(Long projectId, Task.Status status, Task.Type type) {
        List<Task> tasks;

        if (status != null) {
            tasks = taskRepository.findByProjectIdAndStatus(projectId, status);
        } else if (type != null) {
            tasks = taskRepository.findByProjectIdAndType(projectId, type);
        } else {
            tasks = taskRepository.findByProjectId(projectId);
        }

        return enrichAll(tasks);
    }

    // ── Get backlog (no sprint) ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TaskResponse> getBacklog(Long projectId) {
        return enrichAll(taskRepository.findByProjectIdAndSprintIdIsNull(projectId));
    }

    // ── Get by sprint ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TaskResponse> getBySprint(Long sprintId) {
        return enrichAll(taskRepository.findBySprintId(sprintId));
    }

    // ── Get my tasks ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TaskResponse> getMyTasks(Long userId) {
        return enrichAll(taskRepository.findByAssigneeId(userId));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public TaskResponse updateTask(Long taskId, UpdateTaskRequest request, Long userId) {
        Task task = findTask(taskId);
        assertReporterOrAssignee(task, userId);

        if (request.getTitle() != null)       task.setTitle(request.getTitle());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getType() != null)        task.setType(request.getType());
        if (request.getStatus() != null)      task.setStatus(request.getStatus());
        if (request.getPriority() != null)    task.setPriority(request.getPriority());
        if (request.getStoryPoints() != null) task.setStoryPoints(request.getStoryPoints());
        if (request.getDueDate() != null)     task.setDueDate(request.getDueDate());
        if (request.getSprintId() != null)    task.setSprintId(request.getSprintId());

        // assigneeId can be explicitly set to null (unassign)
        if (request.getAssigneeId() != null || isAssigneeExplicitlyCleared(request)) {
            task.setAssigneeId(request.getAssigneeId());
        }

        return enrich(taskRepository.save(task));
    }

    // ── Update status only ────────────────────────────────────────────────────

    @Transactional
    public TaskResponse updateStatus(Long taskId, UpdateStatusRequest request, Long userId) {
        Task task = findTask(taskId);
        assertReporterOrAssignee(task, userId);
        task.setStatus(request.getStatus());
        return enrich(taskRepository.save(task));
    }

    // ── Assign / unassign ─────────────────────────────────────────────────────

    @Transactional
    public TaskResponse assignTask(Long taskId, AssignTaskRequest request, Long userId) {
        Task task = findTask(taskId);
        task.setAssigneeId(request.getAssigneeId());
        return enrich(taskRepository.save(task));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Transactional
    public void deleteTask(Long taskId, Long userId) {
        Task task = findTask(taskId);
        if (!task.getReporterId().equals(userId)) {
            throw new TaskException.UnauthorizedException("Only the task reporter can delete this task");
        }
        taskRepository.delete(task);
        log.info("Task {} deleted by user {}", taskId, userId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Task findTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskException.NotFoundException("Task", id));
    }

    private void assertReporterOrAssignee(Task task, Long userId) {
        boolean isReporter = task.getReporterId().equals(userId);
        boolean isAssignee = userId.equals(task.getAssigneeId());
        if (!isReporter && !isAssignee) {
            throw new TaskException.UnauthorizedException("You don't have permission to modify this task");
        }
    }

    // Checks if assigneeId field was included in request but set to null
    private boolean isAssigneeExplicitlyCleared(UpdateTaskRequest request) {
        return request.getAssigneeId() == null && request.getStatus() != null;
    }

    private TaskResponse enrich(Task task) {
        return enrichAll(List.of(task)).get(0);
    }

    private List<TaskResponse> enrichAll(List<Task> tasks) {
        if (tasks.isEmpty()) return List.of();

        // Collect all unique user IDs (reporters + assignees)
        List<Long> userIds = tasks.stream()
                .flatMap(t -> Stream.of(t.getReporterId(), t.getAssigneeId()))
                .filter(id -> id != null)
                .distinct()
                .toList();

        Map<Long, String> userNames = fetchUserNames(userIds);

        return tasks.stream().map(t -> TaskResponse.builder()
                .id(t.getId())
                .title(t.getTitle())
                .description(t.getDescription())
                .projectId(t.getProjectId())
                .reporterId(t.getReporterId())
                .reporterName(userNames.getOrDefault(t.getReporterId(), "Unknown"))
                .assigneeId(t.getAssigneeId())
                .assigneeName(t.getAssigneeId() != null ? userNames.getOrDefault(t.getAssigneeId(), "Unknown") : null)
                .sprintId(t.getSprintId())
                .type(t.getType().name())
                .status(t.getStatus().name())
                .priority(t.getPriority().name())
                .storyPoints(t.getStoryPoints())
                .dueDate(t.getDueDate())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
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
