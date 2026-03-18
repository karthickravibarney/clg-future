package com.college.erp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String message;
    private String type; // e.g., INFO, WARNING, SUCCESS
    private String targetRole; // ADMIN, STAFF, STUDENT, ALL
    private String targetUserId; // Specific user if not role-based

    private boolean isRead = false;
    private boolean isCleared = false;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
