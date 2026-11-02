package com.maintena.auth.service;

import com.maintena.auth.dto.request.ForgotPasswordRequest;
import com.maintena.auth.dto.request.ResetPasswordRequest;

public interface PasswordResetService {
	void requestReset(ForgotPasswordRequest request);

	void resetPassword(ResetPasswordRequest request);
}
