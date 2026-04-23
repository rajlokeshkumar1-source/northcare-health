package com.northcare.billing.repository;

import com.northcare.billing.model.Payment;
import com.northcare.billing.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByInvoiceId(UUID invoiceId);

    List<Payment> findByPaymentDateAndStatus(LocalDate paymentDate, PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.invoice.id = :invoiceId AND p.status = 'COMPLETED'")
    BigDecimal sumCompletedAmountByInvoiceId(UUID invoiceId);
}
