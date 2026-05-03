package com.gateway.project.taskservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_task_project", columnList = "project_id"),
        @Index(name = "idx_task_assignee", columnList = "assignee_id"),
        @Index(name = "idx_task_sprint", columnList = "sprint_id")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    // References project in project-service (no FK across services)
    @Column(nullable = false)
    private Long projectId;

    // User who created the task
    @Column(nullable = false)
    private Long reporterId;

    // User assigned to the task (nullable — unassigned by default)
    @Column
    private Long assigneeId;

    // Sprint this task belongs to (nullable — backlog by default)
    @Column
    private Long sprintId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Type type = Type.TASK;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.TODO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    // Estimated story points / duration
    @Column
    private Integer storyPoints;

    @Column
    private LocalDate dueDate;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Type {
        TASK,
        BUG,
        STORY,
        EPIC
    }

    public enum Status {
        TODO,
        IN_PROGRESS,
        IN_REVIEW,
        DONE
    }

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}
