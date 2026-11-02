package com.maintena.maintenance.mapper;

import com.maintena.maintenance.dto.request.MaintenanceOrderRequest;
import com.maintena.maintenance.dto.response.MaintenanceOrderResponse;
import com.maintena.maintenance.entity.MaintenanceOrder;
import com.maintena.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface MaintenanceOrderMapper {
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "codigo", ignore = true)
	@Mapping(target = "estado", ignore = true)
	@Mapping(target = "activo", ignore = true)
	@Mapping(target = "solicitante", ignore = true)
	@Mapping(target = "tecnico", ignore = true)
	@Mapping(target = "fechaInicio", ignore = true)
	@Mapping(target = "fechaFin", ignore = true)
	@Mapping(target = "fechaCreacion", ignore = true)
	@Mapping(target = "fechaActualizacion", ignore = true)
	MaintenanceOrder toEntity(MaintenanceOrderRequest request);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "codigo", ignore = true)
	@Mapping(target = "estado", ignore = true)
	@Mapping(target = "activo", ignore = true)
	@Mapping(target = "solicitante", ignore = true)
	@Mapping(target = "tecnico", ignore = true)
	@Mapping(target = "fechaInicio", ignore = true)
	@Mapping(target = "fechaFin", ignore = true)
	@Mapping(target = "fechaCreacion", ignore = true)
	@Mapping(target = "fechaActualizacion", ignore = true)
	void updateEntity(MaintenanceOrderRequest request, @MappingTarget MaintenanceOrder order);

	@Mapping(target = "activoId", source = "activo.id")
	@Mapping(target = "activoCodigo", source = "activo.codigo")
	@Mapping(target = "activoNombre", source = "activo.nombre")
	@Mapping(target = "solicitanteId", source = "solicitante.id")
	@Mapping(target = "solicitanteNombre", source = "solicitante", qualifiedByName = "fullName")
	@Mapping(target = "tecnicoId", source = "tecnico.id")
	@Mapping(target = "tecnicoNombre", source = "tecnico", qualifiedByName = "fullName")
	MaintenanceOrderResponse toResponse(MaintenanceOrder order);

	@Named("fullName")
	default String fullName(User user) {
		if (user == null) return null;
		return (user.getNombre() + " " + user.getApellido()).trim();
	}
}
