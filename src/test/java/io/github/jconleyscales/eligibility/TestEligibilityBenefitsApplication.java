package io.github.jconleyscales.eligibility;

import org.springframework.boot.SpringApplication;

/**
 * Runs the service locally against a throwaway Postgres container, with no compose needed:
 * {@code ./mvnw spring-boot:test-run}.
 */
public class TestEligibilityBenefitsApplication {

	public static void main(String[] args) {
		SpringApplication.from(EligibilityBenefitsApplication::main)
			.with(TestcontainersConfiguration.class)
			.run(args);
	}

}
