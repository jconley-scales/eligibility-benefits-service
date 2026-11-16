package io.github.jconleyscales.eligibility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import io.github.jconleyscales.eligibility.service.EligibilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

/**
 * An error the service didn't expect, such as the database being unreachable: 500 in RFC 9457 Problem Details, the
 * same format as every other error, with a detail that gives away nothing about the cause. The cause goes to the
 * log instead.
 * <p>
 * Replacing {@link EligibilityService} gives this class a Spring context of its own, and so a database container of
 * its own, which is why these tests are kept to one small class.
 */
@IntegrationTest
@ExtendWith(OutputCaptureExtension.class)
class UnexpectedErrorApiTest {

	/** Names a host and port inside the service's network, which a client must never see. */
	private static final String CAUSE = "Connection to eligibility-db.internal:5432 refused";

	@Autowired
	RestTestClient client;

	@MockitoBean
	EligibilityService eligibility;

	@BeforeEach
	void databaseIsUnreachable() {
		given(eligibility.check(any(), any(), any())).willThrow(new DataAccessResourceFailureException(CAUSE));
	}

	/**
	 * The same Problem Details shape as the 400s and 404s. Under {@link JsonCompareMode#STRICT} any extra member,
	 * such as the exception's class, message, or stack trace, fails the test.
	 */
	@Test
	void unexpectedErrorIsAnInternalServerErrorAsProblemDetailsThatRevealNothingAboutTheCause() {
		check()
			.expectStatus().isEqualTo(500)
			.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
			.expectBody().json("""
				{
				  "title": "Internal Server Error",
				  "status": 500,
				  "detail": "The service could not answer this request because of an unexpected error.",
				  "instance": "/v1/eligibility/check"
				}
				""", JsonCompareMode.STRICT);
	}

	@Test
	void unexpectedErrorIsLoggedAtErrorLevelWithTheRequestAndTheCause(CapturedOutput output) {
		check().expectStatus().isEqualTo(500);

		assertThat(output.getOut())
			.containsPattern("ERROR .*Unexpected error answering POST /v1/eligibility/check")
			.contains(DataAccessResourceFailureException.class.getName() + ": " + CAUSE);
	}

	/**
	 * Spring MVC's own errors keep their status rather than becoming a 500: the handler that names their types
	 * matches them more closely than the one for any exception does. Their detail is Spring's wording, so only the
	 * fields that don't depend on it are compared.
	 */
	@ParameterizedTest(name = "{0} as {1}: {3}")
	@CsvSource(delimiter = '|', textBlock = """
			/v1/no-such-path              | application/json | 404 | Not Found
			/v1/members/SYN-0001/coverage | application/json | 405 | Method Not Allowed
			/v1/eligibility/check         | text/plain       | 415 | Unsupported Media Type
			""")
	void springMvcErrorKeepsItsOwnStatusRatherThanBecomingAnUnexpectedError(String path, String contentType,
			int status, String title) {
		client.post().uri(path)
			.contentType(MediaType.parseMediaType(contentType))
			.body("{}")
			.exchange()
			.expectStatus().isEqualTo(status)
			.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
			.expectBody().json("""
				{ "title": "%s", "status": %d, "instance": "%s" }
				""".formatted(title, status, path), JsonCompareMode.LENIENT);
	}

	private ResponseSpec check() {
		return client.post().uri("/v1/eligibility/check")
			.contentType(MediaType.APPLICATION_JSON)
			.body("""
				{ "memberId": "SYN-0001", "procedureCode": "72148", "serviceDate": "2026-03-15" }
				""")
			.exchange();
	}

}
