package com.maintena.user.mapper;

import com.maintena.role.entity.Role;
import com.maintena.user.dto.request.UserRequest;
import com.maintena.user.dto.response.UserResponse;
import com.maintena.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface UserMapper {
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "role", source = "rolId", qualifiedByName = "mapRole")
	@Mapping(target = "password", ignore = true)
	@Mapping(target = "fechaCreacion", ignore = true)
	@Mapping(target = "fechaActualizacion", ignore = true)
	User toEntity(UserRequest request);

	@Mapping(target = "rolId", source = "role.id")
	@Mapping(target = "rolNombre", source = "role.nombre")
	UserResponse toResponse(User entity);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "role", source = "rolId", qualifiedByName = "mapRole")
	@Mapping(target = "password", ignore = true)
	@Mapping(target = "fechaCreacion", ignore = true)
	@Mapping(target = "fechaActualizacion", ignore = true)
	void updateEntity(UserRequest request, @MappingTarget User user);

	@Named("mapRole")
	default Role mapRole(Long id) {
		if (id == null) return null;
		Role r = new Role();
		r.setId(id);
		return r;
	}
}
