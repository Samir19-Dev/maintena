package com.maintena.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maintena.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Flujo HTTP de autenticación")
class AuthFlowIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("un endpoint protegido sin token responde 401")
	void protectedEndpointRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/users"))
			.andExpect(status().isUnauthorized())
			.andExpect(header().exists("X-Correlation-Id"))
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.message").value("No autenticado"));
	}

	@Test
	@DisplayName("el administrador inicia sesión y accede a un endpoint protegido")
	void adminCanLoginAndAccessProtectedEndpoint() throws Exception {
		String token = login("admin@maintena.com", "Admin123*");

		mockMvc.perform(get("/api/v1/users")
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray());
	}

	@Test
	@DisplayName("credenciales inválidas responden 401")
	void badCredentialsReturnUnauthorized() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"email":"admin@maintena.com","password":"incorrecta"}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("Credenciales inválidas"));
	}

	@Test
	@DisplayName("una ruta inexistente responde 404 en formato ProblemDetail")
	void unknownRouteReturnsNotFound() throws Exception {
		String token = login("admin@maintena.com", "Admin123*");

		mockMvc.perform(get("/api/v1/no-existe")
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.message").value("Recurso no encontrado"));
	}

	@Test
	@DisplayName("un JSON mal formado responde 400")
	void malformedJsonReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("Cuerpo de la petición inválido"));
	}

	@Test
	@DisplayName("el contrato OpenAPI es público y contiene las rutas principales")
	void openApiIsPublic() throws Exception {
		MvcResult result = mockMvc.perform(get("/api-docs"))
			.andExpect(status().isOk())
			.andReturn();

		String body = result.getResponse().getContentAsString();
		assertThat(body).contains("/api/v1/auth/login");
		assertThat(body).contains("/api/v1/maintenance/orders");
		assertThat(body).contains("/api/v1/users");
	}

	private String login(String email, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(
					objectMapper.createObjectNode().put("email", email).put("password", password))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andReturn();

		return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
	}
}
