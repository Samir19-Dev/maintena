package com.maintena.role.mapper;

import com.maintena.role.dto.request.RoleRequest;
import com.maintena.role.dto.response.RoleResponse;
import com.maintena.role.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RoleMapper {
	Role toEntity(RoleRequest request);
	RoleResponse toResponse(Role entity);
	void updateEntity(RoleRequest request, @MappingTarget Role role);
}
