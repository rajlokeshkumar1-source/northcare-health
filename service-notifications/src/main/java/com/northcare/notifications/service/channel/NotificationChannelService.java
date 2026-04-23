package com.northcare.notifications.service.channel;

import com.northcare.notifications.exception.NotificationException;
import com.northcare.notifications.model.Notification;

/**
 * Strategy interface for dispatching a notification over a specific transport channel.
 * Implementations: {@link EmailChannelService}, {@link SmsChannelService}, {@link InAppChannelService}.
 */
public interface NotificationChannelService {

    void send(Notification notification) throws NotificationException;
}
