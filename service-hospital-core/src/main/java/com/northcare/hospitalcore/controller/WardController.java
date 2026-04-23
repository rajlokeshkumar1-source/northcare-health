package com.northcare.hospitalcore.controller;

import com.northcare.hospitalcore.dto.WardRequest;
import com.northcare.hospitalcore.dto.WardResponse;
import com.northcare.hospitalcore.service.WardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wards")
@RequiredArgsConstructor
@Tag(name = "Wards", description = "Hospital ward management")
public class WardController {

    private final WardService wardService;

    @GetMapping
    @Operation(summary = "List all wards")
    public ResponseEntity<List<WardResponse>> getAllWards() {
        return ResponseEntity.ok(wardService.getAllWards());
    }

    @PostMapping
    @Operation(summary = "Create a new ward")
    public ResponseEntity<WardResponse> createWard(@Valid @RequestBody WardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(wardService.createWard(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ward by ID")
    public ResponseEntity<WardResponse> getWard(@PathVariable UUID id) {
        return ResponseEntity.ok(wardService.getWardById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update ward details")
    public ResponseEntity<WardResponse> updateWard(
            @PathVariable UUID id,
            @Valid @RequestBody WardRequest request) {
        return ResponseEntity.ok(wardService.updateWard(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a ward")
    public ResponseEntity<Void> deleteWard(@PathVariable UUID id) {
        wardService.deleteWard(id);
        return ResponseEntity.noContent().build();
    }
}
