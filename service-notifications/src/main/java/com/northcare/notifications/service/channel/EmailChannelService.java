package com.northcare.notifications.service.channel;

import com.northcare.notifications.exception.NotificationException;
import com.northcare.notifications.model.Notification;
import com.northcare.notifications.model.NotificationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Simulates email delivery via structured logging.
 * In production: integrate AWS SES or SendGrid by injecting JavaMailSender / SesClient here.
 */
@Service
@Slf4j
public class EmailChannelService implements NotificationChannelService {

    @Override
    public void send(Notification notification) throws NotificationException {
        log.info("📧 EMAIL to {}: {}", notification.getRecipientEmail(), notification.getSubject());
        log.debug("    Body preview: {}",
                notification.getMessage().length() > 100
                        ? notification.getMessage().substring(0, 100) + "…"
                        : notification.getMessage());

        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
    }
}
