package com.maintena.maintenance.service;

import com.maintena.maintenance.dto.request.OrderCommentRequest;
import com.maintena.maintenance.dto.response.OrderCommentResponse;

import java.util.List;

public interface OrderCommentService {
	List<OrderCommentResponse> findAll(Long orderId);

	OrderCommentResponse add(Long orderId, OrderCommentRequest request);

	void delete(Long orderId, Long commentId);
}
