package com.gateway.project.projectservice.repository;

import com.gateway.project.projectservice.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    List<ProjectMember> findByProjectId(Long projectId);

    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    void deleteByProjectIdAndUserId(Long projectId, Long userId);
}
