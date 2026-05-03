package com.gateway.project.taskservice.repository;

import com.gateway.project.taskservice.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectId(Long projectId);

    List<Task> findByProjectIdAndStatus(Long projectId, Task.Status status);

    List<Task> findByProjectIdAndType(Long projectId, Task.Type type);

    List<Task> findByAssigneeId(Long assigneeId);

    List<Task> findBySprintId(Long sprintId);

    // Backlog = tasks in project with no sprint assigned
    List<Task> findByProjectIdAndSprintIdIsNull(Long projectId);

    @Query("SELECT t FROM Task t WHERE t.projectId = :projectId AND t.assigneeId = :assigneeId")
    List<Task> findByProjectIdAndAssigneeId(Long projectId, Long assigneeId);
}
