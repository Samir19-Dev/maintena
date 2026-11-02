package com.maintena.user.service;

import com.maintena.user.dto.request.UserRequest;
import com.maintena.user.dto.response.UserResponse;

import java.util.List;

public interface UserService {
	List<UserResponse> findAll();
	UserResponse findById(Long id);
	UserResponse create(UserRequest request);
	UserResponse update(Long id, UserRequest request);
	void delete(Long id);
}
