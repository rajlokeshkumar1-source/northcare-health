package com.northcare.notifications.controller;

import com.northcare.notifications.dto.*;
import com.northcare.notifications.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Send, receive, and manage notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Send a notification")
    @PostMapping
    public ResponseEntity<NotificationResponse> sendNotification(
            @Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.sendNotification(request));
    }

    @Operation(summary = "Broadcast an emergency alert to multiple recipients on multiple channels")
    @PostMapping("/emergency")
    public ResponseEntity<Map<String, Object>> broadcastEmergency(
            @Valid @RequestBody EmergencyBroadcastRequest request) {
        List<NotificationResponse> results = notificationService.broadcastEmergency(request);
        return ResponseEntity.ok(Map.of(
                "count", results.size(),
                "notifications", results
        ));
    }

    @Operation(summary = "Send a notification using a stored template")
    @PostMapping("/from-template")
    public ResponseEntity<NotificationResponse> sendFromTemplate(
            @Valid @RequestBody TemplateNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.sendFromTemplate(request));
    }

    @Operation(summary = "Get paginated notifications for a recipient")
    @GetMapping("/recipient/{recipientId}")
    public ResponseEntity<Page<NotificationResponse>> getByRecipient(
            @PathVariable UUID recipientId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(notificationService.getByRecipient(recipientId, pageable));
    }

    @Operation(summary = "Mark a notification as read")
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Admin view: list all PENDING notifications in the queue")
    @GetMapping("/pending")
    public ResponseEntity<List<NotificationResponse>> getPending() {
        return ResponseEntity.ok(notificationService.getPendingNotifications());
    }
}
