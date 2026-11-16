package io.github.jconleyscales.eligibility;

import static java.util.stream.Collectors.joining;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

/**
 * {@code POST /v1/eligibility/check} with a request the client must fix: 400 in RFC 9457 Problem Details, the same
 * format as the 404s, with a detail naming the problem. A mistyped procedure code is never answered as no benefit
 * on file. Each test starts from a valid request and breaks one thing.
 */
@IntegrationTest
class EligibilityCheckValidationApiTest {

	private static final String MALFORMED_PROCEDURE_CODE = "procedureCode must be a CPT code (five digits), a CPT "
			+ "Category II or III code (four digits then F, T, or U), or a HCPCS Level II code (an upper-case letter "
			+ "then four digits).";

	@Autowired
	RestTestClient client;

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "memberId", "procedureCode", "serviceDate" })
	void missingFieldIsABadRequestNamingTheField(String field) {
		expectBadRequest(check(validRequestWithout(field)), field + " is required.");
	}

	/** An empty procedure code or Service date is malformed rather than missing; both are tested below. */
	@ParameterizedTest(name = "{0}: {1}")
	@CsvSource(delimiter = '|', quoteCharacter = '`', textBlock = """
			memberId      | null
			memberId      | ""
			memberId      | "   "
			procedureCode | null
			serviceDate   | null
			""")
	void nullOrBlankFieldIsABadRequestNamingTheField(String field, String value) {
		expectBadRequest(check(validRequestWith(field, value)), field + " is required.");
	}

	@Test
	void everyInvalidFieldIsNamedInTheDetailInAlphabeticalOrder() {
		expectBadRequest(check("""
				{ "serviceDate": null, "procedureCode": "7214" }
				"""), "memberId is required. " + MALFORMED_PROCEDURE_CODE + " serviceDate is required.");
	}

	/**
	 * Each would otherwise reach the Plan's Benefits, find none, and come back as no benefit on file. Codes are
	 * upper case, as in the Plans' Benefits, so a lower-case HCPCS code is malformed rather than a different code.
	 */
	@ParameterizedTest(name = "\"{0}\": {1}")
	@CsvSource(delimiter = '|', textBlock = """
			7214      | too few digits
			721480    | too many digits
			72I48     | a letter where a CPT digit belongs
			0042X     | a letter other than F, T, or U after four digits
			E060      | too few digits after the HCPCS letter
			E06011    | too many digits after the HCPCS letter
			e0601     | a lower-case HCPCS letter
			' 72148'  | a leading space
			''        | empty
			""")
	void malformedProcedureCodeIsABadRequestRatherThanNoBenefitOnFile(String procedureCode, String problem) {
		expectBadRequest(check(validRequestWith("procedureCode", "\"" + procedureCode + "\"")),
				MALFORMED_PROCEDURE_CODE);
	}

	/**
	 * Only the format is checked, so a well-formed code the Plan doesn't list reaches the Eligibility check and is
	 * answered there. SYN-0001's Plan, PPO-LOW-DED, has Benefits for 72148 and E0601 but none for the others.
	 */
	@ParameterizedTest(name = "{0}: {2}")
	@CsvSource(delimiter = '|', textBlock = """
			72148 | COVERED            | CPT
			0001F | NO_BENEFIT_ON_FILE | CPT Category II
			0042T | NO_BENEFIT_ON_FILE | CPT Category III
			0001U | NO_BENEFIT_ON_FILE | four digits then U
			E0601 | COVERED            | HCPCS Level II
			""")
	void wellFormedProcedureCodeReachesTheEligibilityCheck(String procedureCode, String benefitStatus,
			String format) {
		check(validRequestWith("procedureCode", "\"" + procedureCode + "\""))
			.expectStatus().isOk()
			.expectBody().json("""
				{
				  "procedureCode": "%s",
				  "coverage": { "active": true, "planId": "PPO-LOW-DED" },
				  "benefit": { "status": "%s" }
				}
				""".formatted(procedureCode, benefitStatus), JsonCompareMode.LENIENT);
	}

	/**
	 * Values are raw JSON, so a string is quoted and a number is not. A date-time would otherwise be cut to its date,
	 * and a number read as a count of days since 1970, so both would get an answer for a date the client never sent.
	 */
	@ParameterizedTest(name = "{0}: {1}")
	@CsvSource(delimiter = '|', quoteCharacter = '`', textBlock = """
			"not-a-date"          | not a date
			"2026-02-30"          | no such day
			"2026-13-01"          | no such month
			"03/15/2026"          | US order
			"2026-3-15"           | single-digit month
			"2026-03-15T10:00:00" | a date-time
			""                    | empty
			20260315              | a number
			""")
	void serviceDateThatIsNotAnIsoDateIsABadRequest(String serviceDate, String problem) {
		expectBadRequest(check(validRequestWith("serviceDate", serviceDate)),
				"serviceDate must be an ISO date (YYYY-MM-DD).");
	}

	@ParameterizedTest(name = "{1}")
	@CsvSource(delimiter = '|', quoteCharacter = '`', textBlock = """
			{ "memberId": "SYN-0001",                                                        | cut off
			{ memberId: "SYN-0001", procedureCode: "29881", serviceDate: "2026-03-15" }       | unquoted names
			{ "memberId": "SYN-0001", "procedureCode": 29881A, "serviceDate": "2026-03-15" } | an unquoted code
			memberId=SYN-0001&procedureCode=29881&serviceDate=2026-03-15                     | a form, not JSON
			""")
	void malformedJsonIsABadRequest(String body, String problem) {
		expectBadRequest(check(body), "The request body is not valid JSON.");
	}

	@ParameterizedTest(name = "{1}")
	@CsvSource(delimiter = '|', quoteCharacter = '`', textBlock = """
			[]           | an array
			"SYN-0001"   | a string
			null         | null
			``           | no body at all
			""")
	void bodyThatIsNotAJsonObjectIsABadRequest(String body, String problem) {
		expectBadRequest(check(body), "The request body must be a JSON object.");
	}

	@ParameterizedTest(name = "{0}: {1}")
	@CsvSource(delimiter = '|', quoteCharacter = '`', textBlock = """
			memberId      | { "id": "SYN-0001" }
			procedureCode | [ "29881" ]
			""")
	void fieldThatIsNotAStringIsABadRequestNamingTheField(String field, String value) {
		expectBadRequest(check(validRequestWith(field, value)), field + " must be a string.");
	}

	/** The same Problem Details shape as the 404s; RFC 9457 lets {@code type} be left out, meaning about:blank. */
	private static void expectBadRequest(ResponseSpec response, String detail) {
		response.expectStatus().isBadRequest()
			.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
			.expectBody().json("""
				{
				  "title": "Bad Request",
				  "status": 400,
				  "detail": "%s",
				  "instance": "/v1/eligibility/check"
				}
				""".formatted(detail), JsonCompareMode.STRICT);
	}

	private ResponseSpec check(String body) {
		return client.post().uri("/v1/eligibility/check")
			.contentType(MediaType.APPLICATION_JSON)
			.body(body)
			.exchange();
	}

	/** @param value raw JSON, written as given, so a test can send a value of any type */
	private static String validRequestWith(String field, String value) {
		var request = validRequest();
		request.put(field, value);
		return json(request);
	}

	private static String validRequestWithout(String field) {
		var request = validRequest();
		request.remove(field);
		return json(request);
	}

	/** Each field's value as raw JSON. */
	private static SequencedMap<String, String> validRequest() {
		var request = new LinkedHashMap<String, String>();
		request.put("memberId", "\"SYN-0001\"");
		request.put("procedureCode", "\"29881\"");
		request.put("serviceDate", "\"2026-03-15\"");
		return request;
	}

	private static String json(SequencedMap<String, String> fields) {
		return fields.entrySet()
			.stream()
			.map(field -> "\"" + field.getKey() + "\": " + field.getValue())
			.collect(joining(", ", "{ ", " }"));
	}

}
