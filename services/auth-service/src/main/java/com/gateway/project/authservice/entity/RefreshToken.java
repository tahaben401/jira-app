package com.gateway.project.authservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "refresh_tokens",
    indexes = {
        @Index(name = "idx_refresh_token_value", columnList = "token"),
        @Index(name = "idx_refresh_token_user",  columnList = "user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
