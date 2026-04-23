package com.northcare.notifications.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplate {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    /** Unique business key, e.g. "APPOINTMENT_REMINDER_24H". */
    @Column(name = "template_code", unique = true, nullable = false, length = 100)
    private String templateCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    /** Subject line with {{placeholder}} syntax. May be null for SMS. */
    @Column(length = 255)
    private String subject;

    /** Body with {{placeholder}} syntax. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
