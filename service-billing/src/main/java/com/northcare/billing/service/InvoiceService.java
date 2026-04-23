package com.northcare.billing.service;

import com.northcare.billing.dto.*;
import com.northcare.billing.exception.InvalidStateException;
import com.northcare.billing.exception.ResourceNotFoundException;
import com.northcare.billing.model.*;
import com.northcare.billing.repository.InvoiceRepository;
import com.northcare.billing.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InvoiceService {

    /** Ontario HST rate */
    private static final BigDecimal TAX_RATE = new BigDecimal("0.13");

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    public InvoiceResponse createInvoice(InvoiceRequest request) {
        List<InvoiceLineItem> lineItems = request.getLineItems().stream()
                .map(li -> InvoiceLineItem.builder()
                        .serviceCode(li.getServiceCode())
                        .description(li.getDescription())
                        .quantity(li.getQuantity())
                        .unitPrice(li.getUnitPrice())
                        .lineTotal(li.getUnitPrice().multiply(BigDecimal.valueOf(li.getQuantity())))
                        .build())
                .collect(Collectors.toList());

        BigDecimal subtotal = lineItems.stream()
                .map(InvoiceLineItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxAmount = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(taxAmount);

        Invoice invoice = Invoice.builder()
                .invoiceNumber(generateInvoiceNumber())
                .patientId(request.getPatientId())
                .patientName(request.getPatientName())
                .serviceDate(request.getServiceDate())
                .dueDate(request.getDueDate())
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .currency("CAD")
                .status(InvoiceStatus.DRAFT)
                .notes(request.getNotes())
                .active(true)
                .build();

        lineItems.forEach(li -> li.setInvoice(invoice));
        invoice.setLineItems(lineItems);

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Created invoice {} for patient {}", saved.getInvoiceNumber(), saved.getPatientId());
        return mapToResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID id) {
        return mapToResponse(findActive(id));
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> listInvoices(Pageable pageable) {
        return invoiceRepository.findByActiveTrue(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> listInvoicesByPatient(UUID patientId, Pageable pageable) {
        return invoiceRepository.findByPatientIdAndActiveTrue(patientId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getOverdueInvoices() {
        List<InvoiceStatus> paid = List.of(
                InvoiceStatus.PAID, InvoiceStatus.CANCELLED, InvoiceStatus.WRITTEN_OFF);
        return invoiceRepository.findOverdueInvoices(LocalDate.now(), paid)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // State transitions
    // -------------------------------------------------------------------------

    public InvoiceResponse issueInvoice(UUID id) {
        Invoice invoice = findActive(id);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new InvalidStateException(
                    "Invoice can only be issued from DRAFT status, current: " + invoice.getStatus());
        }
        invoice.setStatus(InvoiceStatus.ISSUED);
        Invoice saved = invoiceRepository.save(invoice);
        log.info("Issued invoice {}", saved.getInvoiceNumber());
        return mapToResponse(saved);
    }

    public InvoiceResponse processPayment(UUID invoiceId, PaymentRequest request) {
        Invoice invoice = findActive(invoiceId);

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new InvalidStateException("Invoice is already fully paid");
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED
                || invoice.getStatus() == InvoiceStatus.WRITTEN_OFF) {
            throw new InvalidStateException(
                    "Cannot process payment for a " + invoice.getStatus() + " invoice");
        }

        Payment payment = Payment.builder()
                .invoice(invoice)
                .amount(request.getAmount())
                .paymentDate(request.getPaymentDate())
                .paymentMethod(request.getPaymentMethod())
                .referenceNumber(request.getReferenceNumber())
                .status(PaymentStatus.COMPLETED)
                .processedAt(LocalDateTime.now())
                .build();
        paymentRepository.save(payment);

        BigDecimal totalPaid = paymentRepository.sumCompletedAmountByInvoiceId(invoiceId);
        invoice.setStatus(
                totalPaid.compareTo(invoice.getTotalAmount()) >= 0
                        ? InvoiceStatus.PAID
                        : InvoiceStatus.PARTIALLY_PAID);

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Processed payment of {} for invoice {}, new status: {}",
                request.getAmount(), invoice.getInvoiceNumber(), saved.getStatus());
        return mapToResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Reconciliation
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ReconciliationReport reconcile(LocalDate date) {
        List<Payment> payments = paymentRepository.findByPaymentDateAndStatus(date, PaymentStatus.COMPLETED);

        Map<PaymentMethod, BigDecimal> amountByMethod = payments.stream()
                .collect(Collectors.groupingBy(
                        Payment::getPaymentMethod,
                        Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)));

        Map<PaymentMethod, Long> countByMethod = payments.stream()
                .collect(Collectors.groupingBy(Payment::getPaymentMethod, Collectors.counting()));

        BigDecimal total = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ReconciliationReport.builder()
                .date(date)
                .totalTransactions(payments.size())
                .totalAmount(total)
                .amountByMethod(amountByMethod)
                .countByMethod(countByMethod)
                .build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Invoice findActive(UUID id) {
        return invoiceRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
    }

    private String generateInvoiceNumber() {
        Long seq = invoiceRepository.nextInvoiceSequence();
        return String.format("INV-%d-%05d", Year.now().getValue(), seq);
    }

    private InvoiceResponse mapToResponse(Invoice invoice) {
        List<InvoiceResponse.LineItemResponse> items = invoice.getLineItems().stream()
                .map(li -> InvoiceResponse.LineItemResponse.builder()
                        .id(li.getId())
                        .serviceCode(li.getServiceCode())
                        .description(li.getDescription())
                        .quantity(li.getQuantity())
                        .unitPrice(li.getUnitPrice())
                        .lineTotal(li.getLineTotal())
                        .build())
                .collect(Collectors.toList());

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .patientId(invoice.getPatientId())
                .patientName(invoice.getPatientName())
                .serviceDate(invoice.getServiceDate())
                .dueDate(invoice.getDueDate())
                .lineItems(items)
                .subtotal(invoice.getSubtotal())
                .taxAmount(invoice.getTaxAmount())
                .totalAmount(invoice.getTotalAmount())
                .currency(invoice.getCurrency())
                .status(invoice.getStatus())
                .notes(invoice.getNotes())
                .active(invoice.isActive())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }
}
