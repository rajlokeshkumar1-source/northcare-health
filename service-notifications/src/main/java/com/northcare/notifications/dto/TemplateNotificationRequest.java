package com.northcare.notifications.dto;

import com.northcare.notifications.model.RecipientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateNotificationRequest {

    @NotBlank(message = "templateCode is required")
    private String templateCode;

    @NotNull(message = "recipientId is required")
    private UUID recipientId;

    @NotNull(message = "recipientType is required")
    private RecipientType recipientType;

    private String recipientEmail;

    /** Key/value pairs that replace {{placeholders}} in the template body and subject. */
    private Map<String, String> variables;
}
