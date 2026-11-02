package com.maintena.role.service;

import com.maintena.role.dto.request.RoleRequest;
import com.maintena.role.dto.response.RoleResponse;

import java.util.List;

public interface RoleService {
	List<RoleResponse> findAll();
	RoleResponse findById(Long id);
	RoleResponse create(RoleRequest request);
	RoleResponse update(Long id, RoleRequest request);
	void delete(Long id);
}
