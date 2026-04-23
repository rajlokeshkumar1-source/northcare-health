package com.northcare.notifications.controller;

import com.northcare.notifications.exception.NotificationException;
import com.northcare.notifications.model.NotificationTemplate;
import com.northcare.notifications.repository.NotificationTemplateRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
@Tag(name = "Templates", description = "Manage notification message templates")
public class TemplateController {

    private final NotificationTemplateRepository templateRepository;

    @Operation(summary = "List all templates")
    @GetMapping
    public ResponseEntity<List<NotificationTemplate>> listAll() {
        return ResponseEntity.ok(templateRepository.findAll());
    }

    @Operation(summary = "Create a new template")
    @PostMapping
    public ResponseEntity<NotificationTemplate> create(@RequestBody NotificationTemplate template) {
        template.setId(null); // ensure INSERT, not UPDATE
        return ResponseEntity.status(HttpStatus.CREATED).body(templateRepository.save(template));
    }

    @Operation(summary = "Get a template by ID")
    @GetMapping("/{id}")
    public ResponseEntity<NotificationTemplate> getById(@PathVariable UUID id) {
        return templateRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotificationException("Template not found: " + id));
    }

    @Operation(summary = "Update a template")
    @PutMapping("/{id}")
    public ResponseEntity<NotificationTemplate> update(@PathVariable UUID id,
                                                        @RequestBody NotificationTemplate template) {
        if (!templateRepository.existsById(id)) {
            throw new NotificationException("Template not found: " + id);
        }
        template.setId(id);
        return ResponseEntity.ok(templateRepository.save(template));
    }

    @Operation(summary = "Soft-delete a template (marks isActive = false)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new NotificationException("Template not found: " + id));
        template.setIsActive(false);
        templateRepository.save(template);
        return ResponseEntity.noContent().build();
    }
}
