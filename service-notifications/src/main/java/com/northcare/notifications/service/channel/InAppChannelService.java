package com.northcare.notifications.service.channel;

import com.northcare.notifications.exception.NotificationException;
import com.northcare.notifications.model.Notification;
import com.northcare.notifications.model.NotificationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * In-app channel: marks the notification as DELIVERED so the front-end
 * can poll {@code GET /api/v1/notifications/recipient/{id}} to display it.
 */
@Service
@Slf4j
public class InAppChannelService implements NotificationChannelService {

    @Override
    public void send(Notification notification) throws NotificationException {
        log.info("📥 IN_APP notification stored for recipient: {}", notification.getRecipientId());

        notification.setStatus(NotificationStatus.DELIVERED);
        notification.setSentAt(LocalDateTime.now());
    }
}
