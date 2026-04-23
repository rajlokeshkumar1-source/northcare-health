package com.northcare.notifications.service;

import com.northcare.notifications.dto.*;
import com.northcare.notifications.exception.NotificationException;
import com.northcare.notifications.model.*;
import com.northcare.notifications.repository.NotificationRepository;
import com.northcare.notifications.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final ChannelRouter channelRouter;

    // ──────────────────────────────────────────
    //  Core send operations
    // ──────────────────────────────────────────

    @Transactional
    public NotificationResponse sendNotification(NotificationRequest request) {
        String metaJson = request.getMetadata() != null ? request.getMetadata().toString() : null;

        Notification notification = Notification.builder()
                .recipientId(request.getRecipientId())
                .recipientType(request.getRecipientType())
                .recipientEmail(request.getRecipientEmail())
                .channel(request.getChannel())
                .type(request.getType())
                .subject(request.getSubject())
                .message(request.getMessage())
                .priority(request.getPriority() != null ? request.getPriority() : NotificationPriority.NORMAL)
                .status(NotificationStatus.PENDING)
                .scheduledAt(request.getScheduledAt())
                .metadata(metaJson)
                .build();

        notification = notificationRepository.save(notification);

        // Send immediately when there is no future schedule
        boolean sendNow = request.getScheduledAt() == null
                || !request.getScheduledAt().isAfter(LocalDateTime.now());

        if (sendNow) {
            dispatchAndSave(notification);
        }

        return NotificationResponse.from(notification);
    }

    @Transactional
    public NotificationResponse sendFromTemplate(TemplateNotificationRequest request) {
        NotificationTemplate template = templateRepository
                .findByTemplateCodeAndIsActiveTrue(request.getTemplateCode())
                .orElseThrow(() -> new NotificationException(
                        "Template not found or inactive: " + request.getTemplateCode()));

        Map<String, String> vars = request.getVariables() != null ? request.getVariables() : Map.of();
        String subject = replacePlaceholders(template.getSubject(), vars);
        String body    = replacePlaceholders(template.getBody(),    vars);

        NotificationRequest notifRequest = NotificationRequest.builder()
                .recipientId(request.getRecipientId())
                .recipientType(request.getRecipientType())
                .recipientEmail(request.getRecipientEmail())
                .channel(template.getChannel())
                .type(template.getType())
                .subject(subject)
                .message(body)
                .priority(NotificationPriority.NORMAL)
                .build();

        return sendNotification(notifRequest);
    }

    @Transactional
    public List<NotificationResponse> broadcastEmergency(EmergencyBroadcastRequest request) {
        List<NotificationResponse> results = new ArrayList<>();

        for (UUID recipientId : request.getRecipientIds()) {
            for (NotificationChannel channel : request.getChannels()) {
                NotificationRequest req = NotificationRequest.builder()
                        .recipientId(recipientId)
                        .recipientType(RecipientType.STAFF)
                        .channel(channel)
                        .type(NotificationType.EMERGENCY_ALERT)
                        .subject("🚨 EMERGENCY ALERT")
                        .message(request.getMessage())
                        .priority(NotificationPriority.CRITICAL)
                        .build();
                results.add(sendNotification(req));
            }
        }
        return results;
    }

    // ──────────────────────────────────────────
    //  Read / status operations
    // ──────────────────────────────────────────

    @Transactional
    public void markAsRead(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationException("Notification not found: " + id));
        notification.setStatus(NotificationStatus.READ);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getByRecipient(UUID recipientId, Pageable pageable) {
        return notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable)
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getPendingNotifications() {
        return notificationRepository
                .findByStatusAndRetryCountLessThan(NotificationStatus.PENDING, Integer.MAX_VALUE)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    // ──────────────────────────────────────────
    //  Scheduled jobs
    // ──────────────────────────────────────────

    /** Retries FAILED notifications that have not exceeded their maxRetries limit. */
    @Scheduled(fixedDelayString = "${northcare.notifications.retry-delay-ms:300000}")
    @Transactional
    public void retryFailedNotifications() {
        List<Notification> failed = notificationRepository
                .findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 3);

        if (failed.isEmpty()) return;
        log.info("🔁 Retrying {} failed notification(s)…", failed.size());

        for (Notification n : failed) {
            n.setRetryCount(n.getRetryCount() + 1);
            n.setStatus(NotificationStatus.PENDING);
            dispatchAndSave(n);
            log.info("  ↳ notification {} retried (attempt {})", n.getId(), n.getRetryCount());
        }
    }

    /** Sends PENDING notifications whose scheduledAt time has arrived. */
    @Scheduled(fixedDelayString = "${northcare.notifications.scheduled-poll-ms:60000}")
    @Transactional
    public void processScheduled() {
        List<Notification> due = notificationRepository
                .findByScheduledAtBeforeAndStatus(LocalDateTime.now(), NotificationStatus.PENDING);

        if (due.isEmpty()) return;
        log.info("⏰ Processing {} scheduled notification(s)…", due.size());

        for (Notification n : due) {
            dispatchAndSave(n);
            log.info("  ↳ scheduled notification {} dispatched", n.getId());
        }
    }

    // ──────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────

    private void dispatchAndSave(Notification notification) {
        try {
            channelRouter.route(notification);
        } catch (NotificationException ex) {
            log.error("Channel dispatch failed for notification {}: {}", notification.getId(), ex.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailureReason(ex.getMessage());
        }
        notificationRepository.save(notification);
    }

    /** Replaces all {{key}} tokens in a template string with values from the map. */
    private String replacePlaceholders(String template, Map<String, String> variables) {
        if (template == null) return null;
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }
}
