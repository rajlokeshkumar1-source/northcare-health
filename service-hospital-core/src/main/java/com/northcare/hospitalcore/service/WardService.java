package com.northcare.hospitalcore.service;

import com.northcare.hospitalcore.dto.WardRequest;
import com.northcare.hospitalcore.dto.WardResponse;
import com.northcare.hospitalcore.exception.ResourceNotFoundException;
import com.northcare.hospitalcore.mapper.WardMapper;
import com.northcare.hospitalcore.model.Ward;
import com.northcare.hospitalcore.repository.WardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WardService {

    private final WardRepository wardRepository;
    private final WardMapper wardMapper;

    @Transactional(readOnly = true)
    public List<WardResponse> getAllWards() {
        return wardMapper.toResponseList(wardRepository.findAll());
    }

    @Transactional(readOnly = true)
    public WardResponse getWardById(UUID id) {
        Ward ward = wardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ward", id));
        return wardMapper.toResponse(ward);
    }

    @Transactional
    public WardResponse createWard(WardRequest request) {
        Ward ward = wardMapper.toEntity(request);
        Ward saved = wardRepository.save(ward);
        log.info("Created ward id={} name={}", saved.getId(), saved.getName());
        return wardMapper.toResponse(saved);
    }

    @Transactional
    public WardResponse updateWard(UUID id, WardRequest request) {
        Ward ward = wardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ward", id));
        wardMapper.updateEntityFromRequest(request, ward);
        Ward saved = wardRepository.save(ward);
        log.info("Updated ward id={}", saved.getId());
        return wardMapper.toResponse(saved);
    }

    @Transactional
    public void deleteWard(UUID id) {
        if (!wardRepository.existsById(id)) {
            throw new ResourceNotFoundException("Ward", id);
        }
        wardRepository.deleteById(id);
        log.info("Deleted ward id={}", id);
    }
}
