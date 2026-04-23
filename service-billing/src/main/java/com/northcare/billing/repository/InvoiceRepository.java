package com.northcare.billing.repository;

import com.northcare.billing.model.Invoice;
import com.northcare.billing.model.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Page<Invoice> findByActiveTrue(Pageable pageable);

    Page<Invoice> findByPatientIdAndActiveTrue(UUID patientId, Pageable pageable);

    Optional<Invoice> findByIdAndActiveTrue(UUID id);

    @Query("""
            SELECT i FROM Invoice i
            WHERE i.dueDate < :today
              AND i.status NOT IN :excludedStatuses
              AND i.active = true
            """)
    List<Invoice> findOverdueInvoices(LocalDate today, List<InvoiceStatus> excludedStatuses);

    /** Uses the DB sequence to produce a guaranteed-unique monotonic number. */
    @Query(value = "SELECT nextval('invoice_number_seq')", nativeQuery = true)
    Long nextInvoiceSequence();
}
