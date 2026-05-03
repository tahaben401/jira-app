package com.gateway.project.projectservice.repository;

import com.gateway.project.projectservice.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    // Projects owned by a user
    List<Project> findByOwnerId(Long ownerId);

    // Projects where a user is a member
    @Query("SELECT pm.project FROM ProjectMember pm WHERE pm.userId = :userId")
    List<Project> findProjectsByMemberId(Long userId);

    // Projects owned by OR member of
    @Query("""
        SELECT DISTINCT p FROM Project p
        LEFT JOIN p.members pm
        WHERE p.ownerId = :userId OR pm.userId = :userId
    """)
    List<Project> findAllByUserId(Long userId);
}
