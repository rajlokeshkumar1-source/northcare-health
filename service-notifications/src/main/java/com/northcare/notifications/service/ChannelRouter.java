package com.northcare.notifications.service;

import com.northcare.notifications.exception.NotificationException;
import com.northcare.notifications.model.Notification;
import com.northcare.notifications.model.NotificationChannel;
import com.northcare.notifications.model.NotificationStatus;
import com.northcare.notifications.service.channel.EmailChannelService;
import com.northcare.notifications.service.channel.InAppChannelService;
import com.northcare.notifications.service.channel.NotificationChannelService;
import com.northcare.notifications.service.channel.SmsChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

/**
 * Routes a {@link Notification} to the correct channel handler based on
 * {@link NotificationChannel}.  PUSH is handled gracefully (logged + marked FAILED)
 * until a push provider is integrated.
 */
@Service
@Slf4j
public class ChannelRouter {

    private final Map<NotificationChannel, NotificationChannelService> channelServices;

    public ChannelRouter(EmailChannelService email,
                         SmsChannelService sms,
                         InAppChannelService inApp) {
        channelServices = new EnumMap<>(NotificationChannel.class);
        channelServices.put(NotificationChannel.EMAIL,  email);
        channelServices.put(NotificationChannel.SMS,    sms);
        channelServices.put(NotificationChannel.IN_APP, inApp);
    }

    public void route(Notification notification) throws NotificationException {
        NotificationChannelService handler = channelServices.get(notification.getChannel());

        if (handler == null) {
            String msg = "No channel handler registered for: " + notification.getChannel();
            log.warn(msg);
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailureReason(msg);
            throw new NotificationException(msg);
        }

        handler.send(notification);
    }
}
