package com.northcare.hospitalcore.repository;

import com.northcare.hospitalcore.model.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Page<Patient> findAllByIsActiveTrue(Pageable pageable);

    Optional<Patient> findByIdAndIsActiveTrue(UUID id);

    boolean existsByIdAndIsActiveTrue(UUID id);
}
