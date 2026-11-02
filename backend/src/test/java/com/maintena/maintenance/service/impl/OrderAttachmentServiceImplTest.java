package com.maintena.maintenance.service.impl;

import com.maintena.audit.entity.AuditAction;
import com.maintena.audit.service.AuditService;
import com.maintena.exception.BadRequestException;
import com.maintena.exception.ResourceNotFoundException;
import com.maintena.exception.UnauthorizedActionException;
import com.maintena.maintenance.dto.OrderAttachmentFile;
import com.maintena.maintenance.dto.response.OrderAttachmentResponse;
import com.maintena.maintenance.entity.MaintenanceOrder;
import com.maintena.maintenance.entity.OrderAttachment;
import com.maintena.maintenance.mapper.OrderAttachmentMapper;
import com.maintena.maintenance.repository.OrderAttachmentRepository;
import com.maintena.maintenance.service.MaintenanceOrderAccess;
import com.maintena.maintenance.storage.AttachmentStorage;
import com.maintena.role.entity.Role;
import com.maintena.security.current.CurrentUserService;
import com.maintena.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderAttachmentServiceImpl")
class OrderAttachmentServiceImplTest {

	@TempDir
	Path tempDir;

	@Mock
	private OrderAttachmentRepository attachmentRepository;
	@Mock
	private OrderAttachmentMapper attachmentMapper;
	@Mock
	private MaintenanceOrderAccess orderAccess;
	@Mock
	private CurrentUserService currentUserService;
	@Mock
	private AuditService auditService;

	private AttachmentStorage storage;
	private OrderAttachmentServiceImpl service;
	private MaintenanceOrder order;
	private User tecnico;
	private User empleado;

	@BeforeEach
	void setUp() {
		storage = new AttachmentStorage(tempDir.toString(), 5L * 1024 * 1024);
		service = new OrderAttachmentServiceImpl(
			attachmentRepository, attachmentMapper, orderAccess, storage, currentUserService, auditService
		);

		order = new MaintenanceOrder();
		order.setId(50L);
		order.setCodigo("OT-2026-0010");

		tecnico = user(2L, "Carlos", "Tecnico", "TECNICO");
		empleado = user(3L, "Elena", "Empleado", "EMPLEADO");
	}

	@Test
	@DisplayName("sube una imagen válida y audita la acción")
	void uploadStoresImage() {
		when(orderAccess.getVisibleOrder(50L)).thenReturn(order);
		when(currentUserService.getCurrentUser()).thenReturn(tecnico);
		when(attachmentRepository.save(any(OrderAttachment.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
		OrderAttachmentResponse response = new OrderAttachmentResponse();
		when(attachmentMapper.toResponse(any(OrderAttachment.class))).thenReturn(response);

		MockMultipartFile file = new MockMultipartFile(
			"file", "foto.png", "image/png", new byte[]{1, 2, 3}
		);

		assertThat(service.upload(50L, file)).isSameAs(response);

		ArgumentCaptor<OrderAttachment> captor = ArgumentCaptor.forClass(OrderAttachment.class);
		verify(attachmentRepository).save(captor.capture());
		OrderAttachment saved = captor.getValue();

		assertThat(saved.getOrder()).isSameAs(order);
		assertThat(saved.getSubidoPor()).isSameAs(tecnico);
		assertThat(saved.getNombreOriginal()).isEqualTo("foto.png");
		assertThat(saved.getTipoContenido()).isEqualTo("image/png");
		assertThat(saved.getTamano()).isEqualTo(3L);
		assertThat(saved.getNombreAlmacenado()).endsWith(".png");
		assertThat(countFiles()).isEqualTo(1);
		verify(auditService).record(eq(AuditAction.ATTACH), eq("MANTENIMIENTO_ORDEN"), eq(50L), any());
	}

	@Test
	@DisplayName("acepta un PDF y deduce el tipo por la extensión")
	void uploadAcceptsPdf() {
		when(orderAccess.getVisibleOrder(50L)).thenReturn(order);
		when(currentUserService.getCurrentUser()).thenReturn(tecnico);
		when(attachmentRepository.save(any(OrderAttachment.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
		when(attachmentMapper.toResponse(any(OrderAttachment.class))).thenReturn(new OrderAttachmentResponse());

		MockMultipartFile file = new MockMultipartFile(
			"file", "informe.pdf", "application/octet-stream", new byte[]{1, 2}
		);

		service.upload(50L, file);

		ArgumentCaptor<OrderAttachment> captor = ArgumentCaptor.forClass(OrderAttachment.class);
		verify(attachmentRepository).save(captor.capture());
		assertThat(captor.getValue().getTipoContenido()).isEqualTo("application/pdf");
		assertThat(captor.getValue().getNombreAlmacenado()).endsWith(".pdf");
	}

	@Test
	@DisplayName("rechaza extensiones no permitidas")
	void uploadRejectsDisallowedExtension() {
		when(orderAccess.getVisibleOrder(50L)).thenReturn(order);
		when(currentUserService.getCurrentUser()).thenReturn(tecnico);

		MockMultipartFile file = new MockMultipartFile(
			"file", "script.exe", "application/octet-stream", new byte[]{1}
		);

		assertThatThrownBy(() -> service.upload(50L, file))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("Solo se permiten");

		verify(attachmentRepository, never()).save(any());
		assertThat(countFiles()).isZero();
	}

	@Test
	@DisplayName("rechaza archivos que superan el tamaño máximo")
	void uploadRejectsOversizedFile() {
		AttachmentStorage tinyStorage = new AttachmentStorage(tempDir.toString(), 2);
		OrderAttachmentServiceImpl tinyService = new OrderAttachmentServiceImpl(
			attachmentRepository, attachmentMapper, orderAccess, tinyStorage, currentUserService, auditService
		);
		when(orderAccess.getVisibleOrder(50L)).thenReturn(order);
		when(currentUserService.getCurrentUser()).thenReturn(tecnico);

		MockMultipartFile file = new MockMultipartFile(
			"file", "foto.png", "image/png", new byte[]{1, 2, 3}
		);

		assertThatThrownBy(() -> tinyService.upload(50L, file))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("máximo");

		assertThat(countFiles()).isZero();
	}

	@Test
	@DisplayName("descarga un adjunto existente")
	void downloadReturnsFile() {
		AttachmentStorage.StoredAttachment stored = storage.store(50L, imageFile());
		OrderAttachment attachment = attachment(9L, stored, empleado);
		when(orderAccess.getVisibleOrder(50L)).thenReturn(order);
		when(attachmentRepository.findByIdAndOrderId(9L, 50L)).thenReturn(Optional.of(attachment));

		OrderAttachmentFile result = service.download(50L, 9L);

		assertThat(result.resource().exists()).isTrue();
		assertThat(result.nombreOriginal()).isEqualTo("foto.png");
		assertThat(result.tipoContenido()).isEqualTo("image/png");
		assertThat(result.tamano()).isEqualTo(3L);
	}

	@Test
	@DisplayName("falla al descargar un adjunto que no pertenece a la orden")
	void downloadFailsWhenAttachmentDoesNotBelongToOrder() {
		when(orderAccess.getVisibleOrder(50L)).thenReturn(order);
		when(attachmentRepository.findByIdAndOrderId(99L, 50L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.download(50L, 99L))
			.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("el autor puede eliminar el adjunto y se borra del disco")
	void authorCanDeleteAttachment() {
		AttachmentStorage.StoredAttachment stored = storage.store(50L, imageFile());
		OrderAttachment attachment = attachment(9L, stored, empleado);
		when(orderAccess.getVisibleOrder(50L)).thenReturn(order);
		when(attachmentRepository.findByIdAndOrderId(9L, 50L)).thenReturn(Optional.of(attachment));
		when(currentUserService.getCurrentUser()).thenReturn(empleado);
		when(currentUserService.isAdmin()).thenReturn(false);

		service.delete(50L, 9L);

		verify(attachmentRepository).delete(attachment);
		assertThat(countFiles()).isZero();
		verify(auditService).record(eq(AuditAction.DELETE), eq("MANTENIMIENTO_ORDEN"), eq(50L), any());
	}

	@Test
	@DisplayName("otro usuario no puede eliminar un adjunto ajeno")
	void otherUserCannotDeleteAttachment() {
		AttachmentStorage.StoredAttachment stored = storage.store(50L, imageFile());
		OrderAttachment attachment = attachment(9L, stored, empleado);
		when(orderAccess.getVisibleOrder(50L)).thenReturn(order);
		when(attachmentRepository.findByIdAndOrderId(9L, 50L)).thenReturn(Optional.of(attachment));
		when(currentUserService.getCurrentUser()).thenReturn(tecnico);
		when(currentUserService.isAdmin()).thenReturn(false);

		assertThatThrownBy(() -> service.delete(50L, 9L))
			.isInstanceOf(UnauthorizedActionException.class);

		verify(attachmentRepository, never()).delete(any());
		assertThat(countFiles()).isEqualTo(1);
	}

	@Test
	@DisplayName("si falla el guardado en base de datos, borra el archivo subido")
	void uploadCleansFileWhenPersistenceFails() {
		when(orderAccess.getVisibleOrder(50L)).thenReturn(order);
		when(currentUserService.getCurrentUser()).thenReturn(tecnico);
		when(attachmentRepository.save(any(OrderAttachment.class)))
			.thenThrow(new RuntimeException("base de datos caída"));

		assertThatThrownBy(() -> service.upload(50L, imageFile()))
			.hasMessage("base de datos caída");

		assertThat(countFiles()).isZero();
	}

	@Test
	@DisplayName("lista los adjuntos visibles de la orden")
	void findAllReturnsAttachments() {
		OrderAttachment attachment = attachment(9L, new AttachmentStorage.StoredAttachment(
			"archivo.png", "foto.png", "image/png", 3L
		), empleado);
		OrderAttachmentResponse response = new OrderAttachmentResponse();
		when(orderAccess.getVisibleOrder(50L)).thenReturn(order);
		when(attachmentRepository.findByOrderIdOrderByFechaCreacionAsc(50L)).thenReturn(List.of(attachment));
		when(attachmentMapper.toResponse(attachment)).thenReturn(response);

		assertThat(service.findAll(50L)).containsExactly(response);
	}

	private MockMultipartFile imageFile() {
		return new MockMultipartFile("file", "foto.png", "image/png", new byte[]{1, 2, 3});
	}

	private OrderAttachment attachment(Long id, AttachmentStorage.StoredAttachment stored, User subidoPor) {
		OrderAttachment attachment = new OrderAttachment();
		attachment.setId(id);
		attachment.setOrder(order);
		attachment.setSubidoPor(subidoPor);
		attachment.setNombreOriginal(stored.nombreOriginal());
		attachment.setNombreAlmacenado(stored.storedName());
		attachment.setTipoContenido(stored.tipoContenido());
		attachment.setTamano(stored.tamano());
		return attachment;
	}

	private long countFiles() {
		try (Stream<Path> files = Files.walk(tempDir)) {
			return files.filter(Files::isRegularFile).count();
		} catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private User user(Long id, String nombre, String apellido, String roleName) {
		Role role = new Role();
		role.setNombre(roleName);
		User user = new User();
		user.setId(id);
		user.setNombre(nombre);
		user.setApellido(apellido);
		user.setEmail(nombre.toLowerCase() + "@maintena.com");
		user.setRole(role);
		user.setActivo(true);
		return user;
	}
}
