package com.northcare.telehealth.repository;

import com.northcare.telehealth.model.Consultation;
import com.northcare.telehealth.model.ConsultationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, UUID> {

    Page<Consultation> findByPatientId(UUID patientId, Pageable pageable);

    Page<Consultation> findByStatus(ConsultationStatus status, Pageable pageable);

    Page<Consultation> findByScheduledAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    List<Consultation> findByScheduledAtBetweenAndIsActiveTrue(LocalDateTime start, LocalDateTime end);
}
