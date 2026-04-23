package com.northcare.notifications.repository;

import com.northcare.notifications.model.Notification;
import com.northcare.notifications.model.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    List<Notification> findByStatusAndRetryCountLessThan(NotificationStatus status, int maxRetries);

    List<Notification> findByScheduledAtBeforeAndStatus(LocalDateTime time, NotificationStatus status);
}
