package com.maintena.maintenance.controller;

import com.maintena.common.constant.ApiConstants;
import com.maintena.maintenance.dto.OrderAttachmentFile;
import com.maintena.maintenance.dto.request.OrderCommentRequest;
import com.maintena.maintenance.dto.response.OrderAttachmentResponse;
import com.maintena.maintenance.dto.response.OrderCommentResponse;
import com.maintena.maintenance.service.OrderAttachmentService;
import com.maintena.maintenance.service.OrderCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/maintenance/orders/{orderId}")
@RequiredArgsConstructor
@Tag(name = "Maintenance Order Collaboration", description = "Comentarios y adjuntos de las órdenes de trabajo")
public class MaintenanceOrderCollaborationController {
	private final OrderCommentService commentService;
	private final OrderAttachmentService attachmentService;

	@Operation(summary = "Listar los comentarios de una orden")
	@GetMapping("/comments")
	public ResponseEntity<List<OrderCommentResponse>> listComments(@PathVariable Long orderId) {
		return ResponseEntity.ok(commentService.findAll(orderId));
	}

	@Operation(summary = "Agregar un comentario a una orden")
	@PostMapping("/comments")
	public ResponseEntity<OrderCommentResponse> addComment(@PathVariable Long orderId,
														   @Valid @RequestBody OrderCommentRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(commentService.add(orderId, request));
	}

	@Operation(summary = "Eliminar un comentario propio")
	@DeleteMapping("/comments/{commentId}")
	public ResponseEntity<Void> deleteComment(@PathVariable Long orderId, @PathVariable Long commentId) {
		commentService.delete(orderId, commentId);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Listar los adjuntos de una orden")
	@GetMapping("/attachments")
	public ResponseEntity<List<OrderAttachmentResponse>> listAttachments(@PathVariable Long orderId) {
		return ResponseEntity.ok(attachmentService.findAll(orderId));
	}

	@Operation(summary = "Subir un adjunto (imagen o PDF, máximo 5 MB)")
	@PostMapping(value = "/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<OrderAttachmentResponse> uploadAttachment(@PathVariable Long orderId,
																	@RequestParam("file") MultipartFile file) {
		return ResponseEntity.status(HttpStatus.CREATED).body(attachmentService.upload(orderId, file));
	}

	@Operation(summary = "Descargar o visualizar un adjunto")
	@GetMapping("/attachments/{attachmentId}")
	public ResponseEntity<Resource> downloadAttachment(@PathVariable Long orderId,
													   @PathVariable Long attachmentId) {
		OrderAttachmentFile file = attachmentService.download(orderId, attachmentId);
		String filename = URLEncoder.encode(file.nombreOriginal(), StandardCharsets.UTF_8)
			.replace("+", "%20");

		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(file.tipoContenido()))
			.contentLength(file.tamano())
			.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + filename)
			.body(file.resource());
	}

	@Operation(summary = "Eliminar un adjunto propio")
	@DeleteMapping("/attachments/{attachmentId}")
	public ResponseEntity<Void> deleteAttachment(@PathVariable Long orderId,
												 @PathVariable Long attachmentId) {
		attachmentService.delete(orderId, attachmentId);
		return ResponseEntity.noContent().build();
	}
}
