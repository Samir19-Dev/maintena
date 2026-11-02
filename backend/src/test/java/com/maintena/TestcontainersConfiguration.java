package com.maintena;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * MySQL real para las pruebas de integración.
 *
 * <p>Spring Boot arranca y detiene el contenedor automáticamente y conecta el
 * {@code DataSource} mediante {@link ServiceConnection}.</p>
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	MySQLContainer<?> mysqlContainer() {
		return new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
			.withDatabaseName("maintena_test")
			.withUsername("test")
			.withPassword("test");
	}
}
