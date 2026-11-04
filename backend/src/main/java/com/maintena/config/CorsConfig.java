package com.maintena.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {
	@Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://localhost:8081}")
	private String allowedOrigins;

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowCredentials(true);
		config.setAllowedOriginPatterns(Arrays.stream(allowedOrigins.split(","))
			.map(String::trim)
			.filter(origin -> !origin.isEmpty())
			.toList());
		config.setAllowedHeaders(List.of("*"));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setExposedHeaders(List.of("Authorization"));
		// Permite que un frontend público (GitHub Pages) llame al backend en la
		// red local, como exige Chrome con Private Network Access.
		config.setAllowPrivateNetwork(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
