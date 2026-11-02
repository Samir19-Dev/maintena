package com.maintena.auth.service;

import com.maintena.auth.dto.request.ChangePasswordRequest;
import com.maintena.auth.dto.request.LoginRequest;
import com.maintena.auth.dto.request.RefreshTokenRequest;
import com.maintena.auth.dto.request.UpdateProfileRequest;
import com.maintena.auth.dto.response.LoginResponse;
import com.maintena.auth.dto.response.ProfileResponse;

public interface AuthService {
	LoginResponse login(LoginRequest request);

	LoginResponse refresh(RefreshTokenRequest request);

	void logout(String refreshToken);

	ProfileResponse getProfile();

	ProfileResponse updateProfile(UpdateProfileRequest request);

	void changePassword(ChangePasswordRequest request);
}
