package com.maintena.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
	private String accessToken;
	private String refreshToken;
	private String tokenType;
	private Long expiresIn;
	private Long userId;
	private String nombre;
	private String apellido;
	private String email;
	private String rol;
}
