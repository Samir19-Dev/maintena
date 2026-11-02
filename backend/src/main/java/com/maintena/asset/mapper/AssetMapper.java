package com.maintena.asset.mapper;

import com.maintena.asset.dto.request.AssetRequest;
import com.maintena.asset.dto.response.AssetResponse;
import com.maintena.asset.entity.Asset;
import com.maintena.category.entity.Category;
import com.maintena.location.entity.Location;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface AssetMapper {
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "categoria", source = "categoriaId", qualifiedByName = "mapCategoria")
	@Mapping(target = "ubicacion", source = "ubicacionId", qualifiedByName = "mapUbicacion")
	@Mapping(target = "fechaCreacion", ignore = true)
	@Mapping(target = "fechaActualizacion", ignore = true)
	Asset toEntity(AssetRequest request);

	@Mapping(target = "categoriaId", source = "categoria.id")
	@Mapping(target = "categoriaNombre", source = "categoria.nombre")
	@Mapping(target = "ubicacionId", source = "ubicacion.id")
	@Mapping(target = "ubicacionNombre", source = "ubicacion.nombre")
	AssetResponse toResponse(Asset entity);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "categoria", source = "categoriaId", qualifiedByName = "mapCategoria")
	@Mapping(target = "ubicacion", source = "ubicacionId", qualifiedByName = "mapUbicacion")
	@Mapping(target = "fechaCreacion", ignore = true)
	@Mapping(target = "fechaActualizacion", ignore = true)
	void updateEntity(AssetRequest request, @MappingTarget Asset asset);

	@Named("mapCategoria")
	default Category mapCategoria(Long id) {
		if (id == null) return null;
		Category c = new Category();
		c.setId(id);
		return c;
	}

	@Named("mapUbicacion")
	default Location mapUbicacion(Long id) {
		if (id == null) return null;
		Location l = new Location();
		l.setId(id);
		return l;
	}
}
