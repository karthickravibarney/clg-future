package com.college.erp.service;

import com.college.erp.model.Notification;
import com.college.erp.model.UserNotificationState;
import com.college.erp.repository.NotificationRepository;
import com.college.erp.repository.UserNotificationStateRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserNotificationStateRepository userNotificationStateRepository;

    public List<Notification> getGlobalNotifications() {
        return notificationRepository.findAll();
    }

    public List<Notification> getNotificationsForUser(String role, String userId) {
        List<Notification> globalActive = notificationRepository.findActive(userId, role);
        return filterAndOverlayState(globalActive, userId);
    }

    public List<Notification> getArchivedNotificationsForUser(String role, String userId) {
        List<Notification> globalArchived = notificationRepository.findArchived(userId, role);
        return filterAndOverlayState(globalArchived, userId);
    }

    private List<Notification> filterAndOverlayState(List<Notification> notifications, String userId) {
        List<UserNotificationState> states = userNotificationStateRepository.findByUserId(userId);
        Map<Long, UserNotificationState> stateMap = states.stream()
                .collect(Collectors.toMap(UserNotificationState::getNotificationId, s -> s));

        return notifications.stream()
                .filter(n -> {
                    UserNotificationState state = stateMap.get(n.getId());
                    return state == null || !state.isCleared();
                })
                .peek(n -> {
                    UserNotificationState state = stateMap.get(n.getId());
                    if (state != null) {
                        n.setRead(state.isRead());
                    }
                })
                .collect(Collectors.toList());
    }

    public long getUnreadCount(String role, String userId) {
        return getNotificationsForUser(role, userId).stream()
                .filter(n -> !n.isRead())
                .count();
    }

    public void markAsRead(Long notificationId, String userId) {
        if (notificationId == null || userId == null) return;
        UserNotificationState state = userNotificationStateRepository
                .findByUserIdAndNotificationId(userId, notificationId)
                .orElse(UserNotificationState.builder()
                        .userId(userId)
                        .notificationId(notificationId)
                        .build());
        state.setRead(true);
        userNotificationStateRepository.save(state);
    }

    public void clearNotification(Long notificationId, String userId) {
        if (notificationId == null || userId == null) return;
        UserNotificationState state = userNotificationStateRepository
                .findByUserIdAndNotificationId(userId, notificationId)
                .orElse(UserNotificationState.builder()
                        .userId(userId)
                        .notificationId(notificationId)
                        .build());
        state.setCleared(true);
        userNotificationStateRepository.save(state);
    }

    public void clearAllNotifications(String role, String userId) {
        List<Notification> active = getNotificationsForUser(role, userId);
        active.forEach(n -> clearNotification(n.getId(), userId));
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
            notificationRepository.deleteById(notificationId);
        }
    }
}
