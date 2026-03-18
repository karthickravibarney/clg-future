package com.college.erp.service;

import com.college.erp.model.Notification;
import com.college.erp.repository.NotificationRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@Transactional
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public List<Notification> getNotificationsForUser(String role, String userId) {
        return notificationRepository.findActive(userId, role);
    }

    public List<Notification> getArchivedNotificationsForUser(String role, String userId) {
        return notificationRepository.findArchived(userId, role);
    }

    public long getUnreadCount(String role, String userId) {
        return notificationRepository.findActive(userId, role).stream()
                .filter(n -> !n.isRead())
                .count();
    }

    public void markAsRead(Long notificationId) {
        if (notificationId == null) return;
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    public void clearNotification(Long notificationId) {
        if (notificationId == null) return;
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setCleared(true);
            notificationRepository.save(n);
        });
    }

    public void clearAllNotifications(String role, String userId) {
        List<Notification> active = getNotificationsForUser(role, userId);
        active.forEach(n -> n.setCleared(true));
        notificationRepository.saveAll(active);
    }

    public void createNotification(String title, String message, String type, String targetRole) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setTargetRole(targetRole);
        notificationRepository.save(notification);
    }

    public void deleteNotification(Long notificationId) {
        if (notificationId != null) {
            System.out.println("Deleting notification ID: " + notificationId);
            notificationRepository.deleteById(notificationId);
            notificationRepository.flush();
            System.out.println("Deleted notification ID: " + notificationId);
        }
    }
}
