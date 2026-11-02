package com.maintena.maintenance.service.impl;

import com.maintena.audit.entity.AuditAction;
import com.maintena.audit.service.AuditService;
import com.maintena.exception.ResourceNotFoundException;
import com.maintena.exception.UnauthorizedActionException;
import com.maintena.maintenance.dto.request.OrderCommentRequest;
import com.maintena.maintenance.dto.response.OrderCommentResponse;
import com.maintena.maintenance.entity.MaintenanceOrder;
import com.maintena.maintenance.entity.OrderComment;
import com.maintena.maintenance.mapper.OrderCommentMapper;
import com.maintena.maintenance.repository.OrderCommentRepository;
import com.maintena.maintenance.service.MaintenanceOrderAccess;
import com.maintena.maintenance.service.OrderCommentService;
import com.maintena.security.current.CurrentUserService;
import com.maintena.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderCommentServiceImpl implements OrderCommentService {
	private static final String ENTIDAD = "MANTENIMIENTO_ORDEN";

	private final OrderCommentRepository commentRepository;
	private final OrderCommentMapper commentMapper;
	private final MaintenanceOrderAccess orderAccess;
	private final CurrentUserService currentUserService;
	private final AuditService auditService;

	@Override
	@Transactional(readOnly = true)
	public List<OrderCommentResponse> findAll(Long orderId) {
		orderAccess.getVisibleOrder(orderId);
		return commentRepository.findByOrderIdOrderByFechaCreacionAsc(orderId).stream()
			.map(commentMapper::toResponse)
			.collect(Collectors.toList());
	}

	@Override
	public OrderCommentResponse add(Long orderId, OrderCommentRequest request) {
		MaintenanceOrder order = orderAccess.getVisibleOrder(orderId);
		User autor = currentUserService.getCurrentUser();

		OrderComment comment = new OrderComment();
		comment.setOrder(order);
		comment.setAutor(autor);
		comment.setContenido(request.getContenido().trim());

		OrderComment saved = commentRepository.save(comment);
		auditService.record(AuditAction.COMMENT, ENTIDAD, order.getId(),
			"Comentario agregado a la orden " + order.getCodigo());
		return commentMapper.toResponse(saved);
	}

	@Override
	public void delete(Long orderId, Long commentId) {
		MaintenanceOrder order = orderAccess.getVisibleOrder(orderId);
		OrderComment comment = commentRepository.findByIdAndOrderId(commentId, orderId)
			.orElseThrow(() -> new ResourceNotFoundException("Comentario", "id", commentId));

		User actual = currentUserService.getCurrentUser();
		if (!currentUserService.isAdmin() && !comment.getAutor().getId().equals(actual.getId())) {
			throw new UnauthorizedActionException("Solo el autor o un administrador pueden eliminar el comentario");
		}

		auditService.record(AuditAction.DELETE, ENTIDAD, order.getId(),
			"Comentario eliminado de la orden " + order.getCodigo());
		commentRepository.delete(comment);
	}
}
