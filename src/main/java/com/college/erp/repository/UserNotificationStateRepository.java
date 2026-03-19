package com.college.erp.repository;

import com.college.erp.model.UserNotificationState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserNotificationStateRepository extends JpaRepository<UserNotificationState, Long> {
    Optional<UserNotificationState> findByUserIdAndNotificationId(String userId, Long notificationId);
    List<UserNotificationState> findByUserId(String userId);
}
