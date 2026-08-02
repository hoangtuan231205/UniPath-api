package com.example.unipathapi.repository;

import com.example.unipathapi.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId " +
           "AND (:unreadOnly = false OR n.isRead = false) " +
           "ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndUnreadFilter(@Param("userId") Integer userId, @Param("unreadOnly") boolean unreadOnly);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId")
    void markAllAsReadByUserId(@Param("userId") Integer userId);
}
