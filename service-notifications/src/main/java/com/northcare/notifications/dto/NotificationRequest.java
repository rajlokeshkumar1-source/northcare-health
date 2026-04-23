package com.northcare.notifications.dto;

import com.northcare.notifications.model.NotificationChannel;
import com.northcare.notifications.model.NotificationPriority;
import com.northcare.notifications.model.NotificationType;
import com.northcare.notifications.model.RecipientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    @NotNull(message = "recipientId is required")
    private UUID recipientId;

    @NotNull(message = "recipientType is required")
    private RecipientType recipientType;

    private String recipientEmail;

    @NotNull(message = "channel is required")
    private NotificationChannel channel;

    @NotNull(message = "type is required")
    private NotificationType type;

    private String subject;

    @NotBlank(message = "message must not be blank")
    private String message;

    private NotificationPriority priority;

    /** When null or in the past, the notification is sent immediately. */
    private LocalDateTime scheduledAt;

    /** Arbitrary key/value metadata (e.g. appointmentId, invoiceNumber). */
    private Map<String, String> metadata;
}
