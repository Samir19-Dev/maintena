package com.maintena.security.filter;

import com.maintena.security.jwt.JwtService;
import com.maintena.security.userdetails.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

	@Mock
	private JwtService jwtService;
	@Mock
	private CustomUserDetailsService userDetailsService;
	@Mock
	private FilterChain filterChain;

	@InjectMocks
	private JwtAuthenticationFilter filter;

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("autentica una petición con access token válido")
	void authenticatesWithAccessToken() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer access-token");

		UserDetails userDetails = new User(
			"admin@maintena.com",
			"hash",
			List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
		);

		when(jwtService.extractUsername("access-token")).thenReturn("admin@maintena.com");
		when(jwtService.isAccessToken("access-token")).thenReturn(true);
		when(userDetailsService.loadUserByUsername("admin@maintena.com")).thenReturn(userDetails);

		filter.doFilter(request, new MockHttpServletResponse(), filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
		assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
			.isEqualTo("admin@maintena.com");
		assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
			.extracting(Object::toString)
			.containsExactly("ROLE_ADMIN");
		verify(filterChain).doFilter(any(), any());
	}

	@Test
	@DisplayName("no autentica una petición con refresh token como bearer")
	void doesNotAuthenticateWithRefreshToken() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer refresh-token");

		when(jwtService.extractUsername("refresh-token")).thenReturn("admin@maintena.com");
		when(jwtService.isAccessToken("refresh-token")).thenReturn(false);

		filter.doFilter(request, new MockHttpServletResponse(), filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(userDetailsService, never()).loadUserByUsername(anyString());
		verify(filterChain).doFilter(any(), any());
	}

	@Test
	@DisplayName("deja pasar la petición sin cabecera Authorization")
	void passesThroughWithoutHeader() throws Exception {
		filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		verify(jwtService, never()).extractUsername(anyString());
		verify(filterChain).doFilter(any(), any());
	}
}
