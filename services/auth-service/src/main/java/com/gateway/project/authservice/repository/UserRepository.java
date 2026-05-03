package com.gateway.project.authservice.repository;

import com.gateway.project.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAllByRole(User.Role role);

    @Modifying
    @Query("UPDATE User u SET u.enabled = :enabled WHERE u.id = :id")
    void updateEnabledStatus(Long id, boolean enabled);
}
