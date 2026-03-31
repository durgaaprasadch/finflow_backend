package com.finflow.auth.repository;

import com.finflow.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByStatus(String status);
    List<User> findByRoleOrderByCreatedAtDesc(String role);
    List<User> findByRoleAndStatusOrderByCreatedAtDesc(String role, String status);
    List<User> findAllByRole(String role);
    List<User> findAllByRoleAndStatus(String role, String status);
}
