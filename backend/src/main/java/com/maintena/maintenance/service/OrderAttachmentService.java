package com.maintena.maintenance.service;

import com.maintena.maintenance.dto.OrderAttachmentFile;
import com.maintena.maintenance.dto.response.OrderAttachmentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface OrderAttachmentService {
	List<OrderAttachmentResponse> findAll(Long orderId);

	OrderAttachmentResponse upload(Long orderId, MultipartFile file);

	OrderAttachmentFile download(Long orderId, Long attachmentId);

	void delete(Long orderId, Long attachmentId);
}
