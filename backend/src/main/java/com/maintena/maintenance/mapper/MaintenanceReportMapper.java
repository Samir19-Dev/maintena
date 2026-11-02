package com.maintena.maintenance.mapper;

import com.maintena.maintenance.dto.request.MaintenanceReportRequest;
import com.maintena.maintenance.dto.response.MaintenanceReportResponse;
import com.maintena.maintenance.entity.MaintenanceReport;
import com.maintena.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface MaintenanceReportMapper {
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "estado", ignore = true)
	@Mapping(target = "activo", ignore = true)
	@Mapping(target = "solicitante", ignore = true)
	@Mapping(target = "orden", ignore = true)
	@Mapping(target = "fechaCreacion", ignore = true)
	@Mapping(target = "fechaActualizacion", ignore = true)
	MaintenanceReport toEntity(MaintenanceReportRequest request);

	@Mapping(target = "activoId", source = "activo.id")
	@Mapping(target = "activoCodigo", source = "activo.codigo")
	@Mapping(target = "activoNombre", source = "activo.nombre")
	@Mapping(target = "solicitanteId", source = "solicitante.id")
	@Mapping(target = "solicitanteNombre", source = "solicitante", qualifiedByName = "fullName")
	@Mapping(target = "ordenId", source = "orden.id")
	@Mapping(target = "ordenCodigo", source = "orden.codigo")
	MaintenanceReportResponse toResponse(MaintenanceReport report);

	@Named("fullName")
	default String fullName(User user) {
		if (user == null) return null;
		return (user.getNombre() + " " + user.getApellido()).trim();
	}
}
