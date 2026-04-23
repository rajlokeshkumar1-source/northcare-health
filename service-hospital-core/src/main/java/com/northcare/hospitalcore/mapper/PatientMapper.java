package com.northcare.hospitalcore.mapper;

import com.northcare.hospitalcore.dto.PatientRequest;
import com.northcare.hospitalcore.dto.PatientResponse;
import com.northcare.hospitalcore.model.Patient;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {WardMapper.class})
public interface PatientMapper {

    /**
     * Maps Patient entity to PatientResponse DTO.
     * ssnLast4 is explicitly ignored — PHI must never be returned in responses.
     */
    @Mapping(target = "ward", source = "ward")
    PatientResponse toResponse(Patient patient);

    /**
     * Maps PatientRequest DTO to Patient entity.
     * ward and timestamps are handled by the service layer, not here.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ward", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "admissionDate", ignore = true)
    @Mapping(target = "dischargeDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Patient toEntity(PatientRequest request);

    List<PatientResponse> toResponseList(List<Patient> patients);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ward", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "admissionDate", ignore = true)
    @Mapping(target = "dischargeDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(PatientRequest request, @MappingTarget Patient patient);
}
