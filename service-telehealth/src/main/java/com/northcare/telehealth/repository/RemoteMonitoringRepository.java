package com.northcare.telehealth.repository;

import com.northcare.telehealth.model.RemoteMonitoring;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RemoteMonitoringRepository extends JpaRepository<RemoteMonitoring, UUID> {

    Page<RemoteMonitoring> findByPatientIdOrderByRecordedAtDesc(UUID patientId, Pageable pageable);

    List<RemoteMonitoring> findByPatientIdAndIsAlertTrue(UUID patientId);
}
