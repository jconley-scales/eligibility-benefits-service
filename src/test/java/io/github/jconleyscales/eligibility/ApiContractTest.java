package io.github.jconleyscales.eligibility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.json.JsonAssert;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.json.JsonComparison;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;
import tools.jackson.databind.json.JsonMapper;

/**
 * The API contract: the OpenAPI document springdoc generates from the code must match the committed contract
 * file, which the docs page renders and API clients build against. CI runs this test, so a contract that is out
 * of date fails the build.
 * <p>
 * After an intended API change, regenerate the committed file and commit it with the change:
 * {@value #REGENERATE_COMMAND}.
 */
@IntegrationTest
class ApiContractTest {

	/** Relative to the repo root, written with forward slashes so failure messages read the same on every OS. */
	private static final String CONTRACT_FILE = "docs/api/openapi.json";

	private static final String REGENERATE_COMMAND = "./mvnw test -Dtest=ApiContractTest -DregenerateContract";

	@Autowired
	RestTestClient client;

	/**
	 * Tests run on a random port. Taking the server URL from the request would change the contract on every
	 * run, so it is fixed to where {@code docker compose up} serves the API.
	 */
	@Test
	void serverUrlIsFixedSoTheContractIsTheSameOnEveryRun() {
		apiDocs()
			.expectBody().jsonPath("$.servers[*].url").isEqualTo(List.of("http://localhost:8080"));
	}

	/**
	 * Under JSON Schema an {@code enum} lists every allowed value, so a nullable enum must list null too. Otherwise a
	 * response for coverage that isn't active, whose {@code planType} is null, fails validation against the
	 * contract.
	 */
	@Test
	void aNullableEnumListsNullAmongItsValues() {
		apiDocs()
			.expectBody()
			.jsonPath("$.components.schemas.EligibilityCoverage.properties.planType.enum")
			.isEqualTo(Arrays.asList("HMO", "PPO", null))
			.jsonPath("$.components.schemas.PlanBenefitsResponse.properties.planType.enum")
			.isEqualTo(List.of("HMO", "PPO"));
	}

	/**
	 * Every operation documents the 500 that any unexpected error gets. springdoc lists it ahead of the operation's
	 * own responses, and the docs page shows them in the order the contract gives, so they are sorted. The contract
	 * comparison below ignores the order of object keys and so can't catch this.
	 */
	@Test
	void everyOperationListsItsResponsesInStatusCodeOrderIncludingTheServerError() {
		var operations = new JsonMapper().readTree(generatedContract())
			.path("paths")
			.values()
			.stream()
			.flatMap(path -> path.values().stream())
			.toList();
		assertThat(operations).isNotEmpty().allSatisfy(operation -> {
			var statusCodes = List.copyOf(operation.path("responses").propertyNames());
			assertThat(statusCodes).contains("200", "500").isSorted();
		});
	}

	/**
	 * Compares the documents as JSON, so whitespace and the order of object keys don't matter, but array order
	 * does. With {@code -DregenerateContract}, it rewrites the committed file instead of comparing.
	 */
	@Test
	void committedContractMatchesTheCode() throws IOException {
		var generated = generatedContract();
		var committed = Path.of(CONTRACT_FILE);

		if (Boolean.getBoolean("regenerateContract")) {
			Files.writeString(committed, generated);
			return;
		}

		if (!Files.exists(committed)) {
			fail(outOfDate(CONTRACT_FILE + " does not exist."));
		}
		var comparison = JsonAssert.comparator(JsonCompareMode.STRICT).compare(Files.readString(committed), generated);
		if (comparison.getResult() == JsonComparison.Result.MISMATCH) {
			fail(outOfDate("The API the code generates differs from " + CONTRACT_FILE
					+ ". \"Expected\" is the committed file and \"got\" is the code:\n" + comparison.getMessage()));
		}
	}

	private ResponseSpec apiDocs() {
		return client.get().uri("/v3/api-docs").exchange().expectStatus().isOk();
	}

	/**
	 * The live document with LF line endings and a final newline, so the committed file is the same whichever
	 * operating system regenerated it.
	 */
	private String generatedContract() {
		var body = apiDocs().expectBody(String.class).returnResult().getResponseBody();
		return body.replace("\r\n", "\n").stripTrailing() + "\n";
	}

	private static String outOfDate(String problem) {
		return """
				The committed API contract is out of date. %s

				If the API change is intended, regenerate the contract and commit it with the change:

				    %s
				""".formatted(problem, REGENERATE_COMMAND);
	}

}
