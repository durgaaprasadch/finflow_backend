package com.finflow.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private String fullName;

    @Column(name = "mail", unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    private String phone;
    
    private String role; // ADMIN, APPLICANT
    
    private String status; // ACTIVE, ON_HOLD, DISABLED
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private String approvedBy;
    private LocalDateTime approvedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (email != null) email = email.toLowerCase();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

