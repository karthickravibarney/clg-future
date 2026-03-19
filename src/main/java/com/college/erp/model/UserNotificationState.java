package com.college.erp.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_notification_states")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNotificationState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private Long notificationId;

    @Builder.Default
    private boolean isRead = false;

    @Builder.Default
    private boolean isCleared = false;
}
