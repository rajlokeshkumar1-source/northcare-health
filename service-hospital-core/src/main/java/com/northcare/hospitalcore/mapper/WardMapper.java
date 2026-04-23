package com.northcare.hospitalcore.mapper;

import com.northcare.hospitalcore.dto.WardRequest;
import com.northcare.hospitalcore.dto.WardResponse;
import com.northcare.hospitalcore.model.Ward;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WardMapper {

    WardResponse toResponse(Ward ward);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Ward toEntity(WardRequest request);

    List<WardResponse> toResponseList(List<Ward> wards);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(WardRequest request, @MappingTarget Ward ward);
}
