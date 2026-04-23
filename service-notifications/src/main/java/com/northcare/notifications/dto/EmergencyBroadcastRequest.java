package com.northcare.notifications.dto;

import com.northcare.notifications.model.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyBroadcastRequest {

    @NotBlank(message = "message is required")
    private String message;

    @NotEmpty(message = "at least one recipientId is required")
    private List<UUID> recipientIds;

    @NotEmpty(message = "at least one channel is required")
    private List<NotificationChannel> channels;
}
