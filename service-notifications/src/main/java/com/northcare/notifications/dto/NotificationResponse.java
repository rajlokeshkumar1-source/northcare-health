package com.northcare.notifications.dto;

import com.northcare.notifications.model.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID id;
    private UUID recipientId;
    private RecipientType recipientType;
    private String recipientEmail;
    private NotificationChannel channel;
    private NotificationType type;
    private String subject;
    private String message;
    private NotificationPriority priority;
    private NotificationStatus status;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private String failureReason;
    private int retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .recipientId(n.getRecipientId())
                .recipientType(n.getRecipientType())
                .recipientEmail(n.getRecipientEmail())
                .channel(n.getChannel())
                .type(n.getType())
                .subject(n.getSubject())
                .message(n.getMessage())
                .priority(n.getPriority())
                .status(n.getStatus())
                .scheduledAt(n.getScheduledAt())
                .sentAt(n.getSentAt())
                .failureReason(n.getFailureReason())
                .retryCount(n.getRetryCount())
                .createdAt(n.getCreatedAt())
                .updatedAt(n.getUpdatedAt())
                .build();
    }
}
