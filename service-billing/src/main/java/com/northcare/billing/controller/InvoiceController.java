package com.northcare.billing.controller;

import com.northcare.billing.dto.*;
import com.northcare.billing.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Invoice management and payment processing")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    @Operation(summary = "List all invoices (paginated)")
    public ResponseEntity<Page<InvoiceResponse>> listInvoices(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(invoiceService.listInvoices(pageable));
    }

    @PostMapping
    @Operation(summary = "Create a new invoice (starts in DRAFT)")
    public ResponseEntity<InvoiceResponse> createInvoice(
            @Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.createInvoice(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get invoice by ID")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.getInvoice(id));
    }

    @PutMapping("/{id}/issue")
    @Operation(summary = "Issue a draft invoice (DRAFT → ISSUED)")
    public ResponseEntity<InvoiceResponse> issueInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.issueInvoice(id));
    }

    @PostMapping("/{id}/payments")
    @Operation(summary = "Record a payment against an invoice")
    public ResponseEntity<InvoiceResponse> processPayment(
            @PathVariable UUID id,
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.processPayment(id, request));
    }

    @GetMapping("/overdue")
    @Operation(summary = "List all overdue invoices (dueDate < today, not paid/cancelled/written-off)")
    public ResponseEntity<List<InvoiceResponse>> getOverdueInvoices() {
        return ResponseEntity.ok(invoiceService.getOverdueInvoices());
    }

    @GetMapping("/reconciliation")
    @Operation(summary = "Daily reconciliation report — total payments grouped by method")
    public ResponseEntity<ReconciliationReport> reconcile(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(invoiceService.reconcile(date));
    }
}
