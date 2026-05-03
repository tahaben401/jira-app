package com.gateway.project.projectservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "project_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "user_id"})
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // References user in auth-service (no FK across services)
    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime joinedAt;

    public enum Role {
        DEVELOPER,
        QA,
        SCRUM_MASTER,
        PRODUCT_OWNER
    }
}
