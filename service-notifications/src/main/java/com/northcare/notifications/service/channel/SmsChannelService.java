package com.northcare.notifications.service.channel;

import com.northcare.notifications.exception.NotificationException;
import com.northcare.notifications.model.Notification;
import com.northcare.notifications.model.NotificationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Simulates SMS delivery via structured logging.
 * In production: integrate AWS SNS or Twilio by injecting SnsClient / TwilioRestClient here.
 */
@Service
@Slf4j
public class SmsChannelService implements NotificationChannelService {

    private static final int SMS_MAX_LENGTH = 160;

    @Override
    public void send(Notification notification) throws NotificationException {
        String body = notification.getMessage();
        if (body.length() > SMS_MAX_LENGTH) {
            body = body.substring(0, SMS_MAX_LENGTH);
        }
        log.info("📱 SMS to {}: {}", notification.getRecipientEmail(), body);

        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
    }
}
