// src/main/java/com/mms/entity/User.java
package com.mms.entity;

import com.mms.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false, length = 32)
    private String username;

    @Column(nullable = false, length = 80)
    private String fullName;

    @Column(unique = true, length = 254)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean active = true;

    @Column(length = 2000)
    private String photoUrl;

    @Column(length = 2000)
    private String bio;

    @Column(name = "two_factor_enabled", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean twoFactorEnabled = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Helper methods for boolean checks
    public boolean isActive() {
        return active;
    }

    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }
}