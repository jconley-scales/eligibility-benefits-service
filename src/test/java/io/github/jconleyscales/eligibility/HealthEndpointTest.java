package io.github.jconleyscales.eligibility;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
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
