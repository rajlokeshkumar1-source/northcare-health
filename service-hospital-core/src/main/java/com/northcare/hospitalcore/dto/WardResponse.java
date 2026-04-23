package com.northcare.hospitalcore.dto;

import com.northcare.hospitalcore.model.Ward.WardType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class WardResponse {

    private UUID id;
    private String name;
    private WardType wardType;
    private int floor;
    private int bedCount;
    private int availableBeds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
