package com.northcare.telehealth.service;

import com.northcare.telehealth.dto.ConsultationRequest;
import com.northcare.telehealth.dto.ConsultationResponse;
import com.northcare.telehealth.exception.InvalidStateTransitionException;
import com.northcare.telehealth.exception.ResourceNotFoundException;
import com.northcare.telehealth.model.Consultation;
import com.northcare.telehealth.model.ConsultationStatus;
import com.northcare.telehealth.repository.ConsultationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultationService {

    private final ConsultationRepository consultationRepository;

    @Transactional
    public ConsultationResponse scheduleConsultation(ConsultationRequest request) {
        String meetingUrl = "https://meet.northcare.health/" + UUID.randomUUID();

        Consultation consultation = Consultation.builder()
                .patientId(request.getPatientId())
                .doctorId(request.getDoctorId())
                .doctorName(request.getDoctorName())
                .scheduledAt(request.getScheduledAt())
                .status(ConsultationStatus.SCHEDULED)
                .consultationType(request.getConsultationType())
                .chiefComplaint(request.getChiefComplaint())
                .meetingUrl(meetingUrl)
                .isActive(Boolean.TRUE)
                .build();

        Consultation saved = consultationRepository.save(consultation);
        log.info("Consultation scheduled: id={}, patientId={}, doctorId={}",
                saved.getId(), saved.getPatientId(), saved.getDoctorId());
        return toResponse(saved, true);
    }

    @Transactional
    public ConsultationResponse startConsultation(UUID id) {
        Consultation consultation = findById(id);

        if (consultation.getStatus() != ConsultationStatus.SCHEDULED) {
            throw new InvalidStateTransitionException(
                    "Cannot start consultation in status: " + consultation.getStatus());
        }

        consultation.setStatus(ConsultationStatus.IN_PROGRESS);
        consultation.setActualStartAt(LocalDateTime.now());

        Consultation saved = consultationRepository.save(consultation);
        log.info("Consultation started: id={}", id);
        return toResponse(saved, true);
    }

    @Transactional
    public ConsultationResponse completeConsultation(UUID id, String notes) {
        Consultation consultation = findById(id);

        if (consultation.getStatus() != ConsultationStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException(
                    "Cannot complete consultation in status: " + consultation.getStatus());
        }

        LocalDateTime endTime = LocalDateTime.now();
        consultation.setStatus(ConsultationStatus.COMPLETED);
        consultation.setActualEndAt(endTime);
        consultation.setNotes(notes);

        if (consultation.getActualStartAt() != null) {
            consultation.setDurationMinutes(
                    (int) ChronoUnit.MINUTES.between(consultation.getActualStartAt(), endTime));
        }

        Consultation saved = consultationRepository.save(consultation);
        log.info("Consultation completed: id={}, duration={}min", id, saved.getDurationMinutes());
        return toResponse(saved, true);
    }

    @Transactional
    public ConsultationResponse cancelConsultation(UUID id, String reason) {
        Consultation consultation = findById(id);

        if (consultation.getStatus() == ConsultationStatus.COMPLETED
                || consultation.getStatus() == ConsultationStatus.CANCELLED) {
            throw new InvalidStateTransitionException(
                    "Cannot cancel consultation in status: " + consultation.getStatus());
        }

        consultation.setStatus(ConsultationStatus.CANCELLED);
        consultation.setNotes(reason);
        consultation.setIsActive(Boolean.FALSE);

        Consultation saved = consultationRepository.save(consultation);
        log.info("Consultation cancelled: id={}", id);
        return toResponse(saved, false);
    }

    @Transactional(readOnly = true)
    public Page<ConsultationResponse> getConsultationsByPatient(UUID patientId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("scheduledAt").descending());
        // PHI: notes excluded from list responses
        return consultationRepository.findByPatientId(patientId, pageRequest)
                .map(c -> toResponse(c, false));
    }

    @Transactional(readOnly = true)
    public ConsultationResponse getConsultationById(UUID id) {
        // PHI: notes included in single-record response
        return toResponse(findById(id), true);
    }

    @Transactional(readOnly = true)
    public List<ConsultationResponse> getTodayConsultations() {
        LocalDateTime startOfDay = LocalDate.now().atTime(LocalTime.MIN);
        LocalDateTime endOfDay   = LocalDate.now().atTime(LocalTime.MAX);
        return consultationRepository
                .findByScheduledAtBetweenAndIsActiveTrue(startOfDay, endOfDay)
                .stream()
                .map(c -> toResponse(c, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ConsultationResponse> getConsultationsByStatus(ConsultationStatus status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("scheduledAt").descending());
        return consultationRepository.findByStatus(status, pageRequest)
                .map(c -> toResponse(c, false));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Consultation findById(UUID id) {
        return consultationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation not found with id: " + id));
    }

    /**
     * Maps entity → response DTO.
     *
     * @param includeNotes when true the PHI notes field is populated;
     *                     callers should set this only for single-record endpoints.
     */
    private ConsultationResponse toResponse(Consultation c, boolean includeNotes) {
        return ConsultationResponse.builder()
                .id(c.getId())
                .patientId(c.getPatientId())
                .doctorId(c.getDoctorId())
                .doctorName(c.getDoctorName())
                .scheduledAt(c.getScheduledAt())
                .actualStartAt(c.getActualStartAt())
                .actualEndAt(c.getActualEndAt())
                .status(c.getStatus())
                .consultationType(c.getConsultationType())
                .chiefComplaint(c.getChiefComplaint())
                .notes(includeNotes ? c.getNotes() : null)
                .meetingUrl(c.getMeetingUrl())
                .durationMinutes(c.getDurationMinutes())
                .isActive(c.getIsActive())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
