package com.northcare.hospitalcore.service;

import com.northcare.hospitalcore.dto.PatientRequest;
import com.northcare.hospitalcore.dto.PatientResponse;
import com.northcare.hospitalcore.exception.ResourceNotFoundException;
import com.northcare.hospitalcore.mapper.PatientMapper;
import com.northcare.hospitalcore.model.Patient;
import com.northcare.hospitalcore.model.Ward;
import com.northcare.hospitalcore.repository.PatientRepository;
import com.northcare.hospitalcore.repository.WardRepository;
import com.northcare.hospitalcore.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final WardRepository wardRepository;
    private final PatientMapper patientMapper;
    private final EncryptionUtil encryptionUtil;

    @Transactional(readOnly = true)
    public Page<PatientResponse> getAllPatients(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return patientRepository.findAllByIsActiveTrue(pageable)
                .map(patientMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PatientResponse getPatientById(UUID id) {
        Patient patient = patientRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", id));
        return patientMapper.toResponse(patient);
    }

    @Transactional
    public PatientResponse createPatient(PatientRequest request) {
        Patient patient = patientMapper.toEntity(request);

        // Encrypt PHI before persistence
        if (request.getSsnLast4() != null) {
            patient.setSsnLast4(encryptionUtil.encrypt(request.getSsnLast4()));
        }

        // Resolve ward if provided
        if (request.getWardId() != null) {
            Ward ward = wardRepository.findById(request.getWardId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ward", request.getWardId()));
            patient.setWard(ward);
        }

        patient.setActive(true);
        Patient saved = patientRepository.save(patient);
        log.info("Created patient id={}", saved.getId());
        return patientMapper.toResponse(saved);
    }

    @Transactional
    public PatientResponse updatePatient(UUID id, PatientRequest request) {
        Patient patient = patientRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", id));

        patientMapper.updateEntityFromRequest(request, patient);

        // Re-encrypt PHI if updated
        if (request.getSsnLast4() != null) {
            patient.setSsnLast4(encryptionUtil.encrypt(request.getSsnLast4()));
        }

        // Update ward reference if changed
        if (request.getWardId() != null) {
            Ward ward = wardRepository.findById(request.getWardId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ward", request.getWardId()));
            patient.setWard(ward);
        }

        Patient saved = patientRepository.save(patient);
        log.info("Updated patient id={}", saved.getId());
        return patientMapper.toResponse(saved);
    }

    @Transactional
    public void deletePatient(UUID id) {
        Patient patient = patientRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", id));
        // Soft delete — HIPAA requires audit trail, records are never physically removed
        patient.setActive(false);
        patientRepository.save(patient);
        log.info("Soft-deleted patient id={}", id);
    }
}
