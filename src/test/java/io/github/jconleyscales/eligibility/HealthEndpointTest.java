package io.github.jconleyscales.eligibility;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.client.RestTestClient;

@IntegrationTest
class HealthEndpointTest {

	@Autowired
	RestTestClient client;

	@Test
	void healthIsUpOnceTheServiceIsRunningAgainstPostgres() {
		client.get().uri("/actuator/health")
			.exchange()
			.expectStatus().isOk()
			.expectBody().jsonPath("$.status").isEqualTo("UP");
	}

}
