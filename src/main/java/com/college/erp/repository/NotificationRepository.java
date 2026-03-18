package com.college.erp.repository;

import com.college.erp.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("SELECT n FROM Notification n WHERE (n.targetUserId = :userId OR n.targetRole = :role OR n.targetRole = 'ALL') AND n.isCleared = false ORDER BY n.createdAt DESC")
    List<Notification> findActive(@Param("userId") String userId, @Param("role") String role);

    long countByTargetRoleAndIsClearedFalseAndIsReadFalse(String role);

    @Query("SELECT n FROM Notification n WHERE (n.targetUserId = :userId OR n.targetRole = :role OR n.targetRole = 'ALL') AND n.isCleared = true ORDER BY n.createdAt DESC")
    List<Notification> findArchived(@Param("userId") String userId, @Param("role") String role);
}
