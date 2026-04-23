package com.northcare.hospitalcore.dto;

import com.northcare.hospitalcore.model.Ward.WardType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WardRequest {

    @NotBlank(message = "Ward name is required")
    @Size(max = 100)
    private String name;

    @NotNull(message = "Ward type is required")
    private WardType wardType;

    @NotNull(message = "Floor is required")
    @Min(value = 1, message = "Floor must be at least 1")
    private Integer floor;

    @NotNull(message = "Bed count is required")
    @Min(value = 1, message = "Bed count must be at least 1")
    private Integer bedCount;

    @NotNull(message = "Available beds is required")
    @Min(value = 0, message = "Available beds cannot be negative")
    private Integer availableBeds;
}
