package com.gateway.project.sprintservice.repository;

import com.gateway.project.sprintservice.entity.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SprintRepository extends JpaRepository<Sprint, Long> {

    List<Sprint> findByProjectId(Long projectId);

    Optional<Sprint> findByProjectIdAndStatus(Long projectId, Sprint.Status status);

    boolean existsByProjectIdAndStatus(Long projectId, Sprint.Status status);
}
