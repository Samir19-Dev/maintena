package com.maintena.location.mapper;

import com.maintena.location.dto.request.LocationRequest;
import com.maintena.location.dto.response.LocationResponse;
import com.maintena.location.entity.Location;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface LocationMapper {
	Location toEntity(LocationRequest request);
	LocationResponse toResponse(Location location);
	void updateEntity(LocationRequest request, @MappingTarget Location location);
}
