package com.maintena.maintenance.service.impl;

import com.maintena.audit.entity.AuditAction;
import com.maintena.audit.service.AuditService;
import com.maintena.exception.ResourceNotFoundException;
import com.maintena.exception.UnauthorizedActionException;
import com.maintena.maintenance.dto.OrderAttachmentFile;
import com.maintena.maintenance.dto.response.OrderAttachmentResponse;
import com.maintena.maintenance.entity.MaintenanceOrder;
import com.maintena.maintenance.entity.OrderAttachment;
import com.maintena.maintenance.mapper.OrderAttachmentMapper;
import com.maintena.maintenance.repository.OrderAttachmentRepository;
import com.maintena.maintenance.service.MaintenanceOrderAccess;
import com.maintena.maintenance.service.OrderAttachmentService;
import com.maintena.maintenance.storage.AttachmentStorage;
import com.maintena.security.current.CurrentUserService;
import com.maintena.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderAttachmentServiceImpl implements OrderAttachmentService {
	private static final String ENTIDAD = "MANTENIMIENTO_ORDEN";
	private static final int MAX_DETALLE = 400;

	private final OrderAttachmentRepository attachmentRepository;
	private final OrderAttachmentMapper attachmentMapper;
	private final MaintenanceOrderAccess orderAccess;
	private final AttachmentStorage attachmentStorage;
	private final CurrentUserService currentUserService;
	private final AuditService auditService;

	@Override
	@Transactional(readOnly = true)
	public List<OrderAttachmentResponse> findAll(Long orderId) {
		orderAccess.getVisibleOrder(orderId);
		return attachmentRepository.findByOrderIdOrderByFechaCreacionAsc(orderId).stream()
			.map(attachmentMapper::toResponse)
			.collect(Collectors.toList());
	}

	@Override
	public OrderAttachmentResponse upload(Long orderId, MultipartFile file) {
		MaintenanceOrder order = orderAccess.getVisibleOrder(orderId);
		User usuario = currentUserService.getCurrentUser();

		AttachmentStorage.StoredAttachment stored = attachmentStorage.store(orderId, file);

		OrderAttachment attachment = new OrderAttachment();
		attachment.setOrder(order);
		attachment.setSubidoPor(usuario);
		attachment.setNombreOriginal(stored.nombreOriginal());
		attachment.setNombreAlmacenado(stored.storedName());
		attachment.setTipoContenido(stored.tipoContenido());
		attachment.setTamano(stored.tamano());

		OrderAttachment saved;
		try {
			saved = attachmentRepository.save(attachment);
		} catch (RuntimeException ex) {
			attachmentStorage.delete(orderId, stored.storedName());
			throw ex;
		}

		auditService.record(AuditAction.ATTACH, ENTIDAD, order.getId(),
			truncate("Adjunto " + stored.nombreOriginal() + " agregado a la orden " + order.getCodigo()));
		return attachmentMapper.toResponse(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public OrderAttachmentFile download(Long orderId, Long attachmentId) {
		orderAccess.getVisibleOrder(orderId);
		OrderAttachment attachment = getAttachment(orderId, attachmentId);
		Resource resource = attachmentStorage.load(orderId, attachment.getNombreAlmacenado());
		return new OrderAttachmentFile(
			resource,
			attachment.getNombreOriginal(),
			attachment.getTipoContenido(),
			attachment.getTamano()
		);
	}

	@Override
	public void delete(Long orderId, Long attachmentId) {
		MaintenanceOrder order = orderAccess.getVisibleOrder(orderId);
		OrderAttachment attachment = getAttachment(orderId, attachmentId);

		User actual = currentUserService.getCurrentUser();
		if (!currentUserService.isAdmin() && !attachment.getSubidoPor().getId().equals(actual.getId())) {
			throw new UnauthorizedActionException("Solo quien subió el adjunto o un administrador pueden eliminarlo");
		}

		attachmentRepository.delete(attachment);
		attachmentStorage.delete(orderId, attachment.getNombreAlmacenado());
		auditService.record(AuditAction.DELETE, ENTIDAD, order.getId(),
			truncate("Adjunto " + attachment.getNombreOriginal() + " eliminado de la orden " + order.getCodigo()));
	}

	private OrderAttachment getAttachment(Long orderId, Long attachmentId) {
		return attachmentRepository.findByIdAndOrderId(attachmentId, orderId)
			.orElseThrow(() -> new ResourceNotFoundException("Adjunto", "id", attachmentId));
	}

	private String truncate(String detalle) {
		if (detalle == null || detalle.length() <= MAX_DETALLE) {
			return detalle;
		}
		return detalle.substring(0, MAX_DETALLE);
	}
}
