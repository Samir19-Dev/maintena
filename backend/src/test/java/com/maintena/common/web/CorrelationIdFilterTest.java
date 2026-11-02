package com.maintena.common.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CorrelationIdFilter")
class CorrelationIdFilterTest {

	private final CorrelationIdFilter filter = new CorrelationIdFilter();

	@Test
	@DisplayName("genera un id, lo expone en la respuesta y lo limpia del MDC")
	void generatesCorrelationId() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<String> duringRequest = new AtomicReference<>();

		FilterChain chain = (req, res) -> duringRequest.set(MDC.get(CorrelationIdFilter.MDC_KEY));
		filter.doFilter(request, response, chain);

		String header = response.getHeader(CorrelationIdFilter.HEADER);
		assertThat(header).isNotBlank();
		assertThat(duringRequest.get()).isEqualTo(header);
		assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
	}

	@Test
	@DisplayName("respeta el id enviado por el cliente")
	void reusesIncomingCorrelationId() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(CorrelationIdFilter.HEADER, "prueba-123");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, (req, res) -> { });

		assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("prueba-123");
	}
}
