package com.maintena.maintenance.mapper;

import com.maintena.maintenance.dto.response.OrderCommentResponse;
import com.maintena.maintenance.entity.OrderComment;
import com.maintena.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface OrderCommentMapper {
	@Mapping(target = "orderId", source = "order.id")
	@Mapping(target = "autorId", source = "autor.id")
	@Mapping(target = "autorNombre", source = "autor", qualifiedByName = "fullName")
	OrderCommentResponse toResponse(OrderComment comment);

	@Named("fullName")
	default String fullName(User user) {
		if (user == null) return null;
		return (user.getNombre() + " " + user.getApellido()).trim();
	}
}
