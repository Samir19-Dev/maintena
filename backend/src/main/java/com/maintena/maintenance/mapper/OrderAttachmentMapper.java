package com.maintena.maintenance.mapper;

import com.maintena.maintenance.dto.response.OrderAttachmentResponse;
import com.maintena.maintenance.entity.OrderAttachment;
import com.maintena.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface OrderAttachmentMapper {
	@Mapping(target = "orderId", source = "order.id")
	@Mapping(target = "subidoPorId", source = "subidoPor.id")
	@Mapping(target = "subidoPorNombre", source = "subidoPor", qualifiedByName = "fullName")
	OrderAttachmentResponse toResponse(OrderAttachment attachment);

	@Named("fullName")
	default String fullName(User user) {
		if (user == null) return null;
		return (user.getNombre() + " " + user.getApellido()).trim();
	}
}
