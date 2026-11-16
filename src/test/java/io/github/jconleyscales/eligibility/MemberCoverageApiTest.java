package io.github.jconleyscales.eligibility;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

/**
 * {@code GET /v1/members/{memberId}/coverage}: a Member's coverage history, read from the seed data.
 */
@IntegrationTest
class MemberCoverageApiTest {

	@Autowired
	RestTestClient client;

	@Test
	void memberWithOneOngoingCoverageHasANullEndDate() {
		coverageOf("SYN-0001")
			.expectStatus().isOk()
			.expectHeader().contentType(MediaType.APPLICATION_JSON)
			.expectBody().json("""
				{
				  "memberId": "SYN-0001",
				  "coverages": [
				    {
				      "planId": "PPO-LOW-DED",
				      "planName": "Synthetic Low-Deductible PPO",
				      "planType": "PPO",
				      "startDate": "2025-01-01",
				      "endDate": null
				    }
				  ]
				}
				""", JsonCompareMode.STRICT);
	}

	@Test
	void planSwitchMemberHasBackToBackCoveragesOrderedByStartDate() {
		coverageOf("SYN-0007")
			.expectStatus().isOk()
			.expectBody().json("""
				{
				  "memberId": "SYN-0007",
				  "coverages": [
				    {
				      "planId": "PPO-HIGH-DED",
				      "planName": "Synthetic High-Deductible PPO",
				      "planType": "PPO",
				      "startDate": "2026-01-01",
				      "endDate": "2026-06-30"
				    },
				    {
				      "planId": "HMO-STANDARD",
				      "planName": "Synthetic Standard HMO",
				      "planType": "HMO",
				      "startDate": "2026-07-01",
				      "endDate": null
				    }
				  ]
				}
				""", JsonCompareMode.STRICT);
	}

	@Test
	void lapsedMemberStillHasTheirEndedCoverage() {
		coverageOf("SYN-0004")
			.expectStatus().isOk()
			.expectBody().json("""
				{
				  "memberId": "SYN-0004",
				  "coverages": [
				    {
				      "planId": "PPO-LOW-DED",
				      "planName": "Synthetic Low-Deductible PPO",
				      "planType": "PPO",
				      "startDate": "2023-01-01",
				      "endDate": "2025-12-31"
				    }
				  ]
				}
				""", JsonCompareMode.STRICT);
	}

	/**
	 * The rest of the edge-case Members the service starts with; SYN-0001, SYN-0004, and SYN-0007 are tested
	 * above. Together they use all three seed Plans. The README's seed table lists every Member.
	 */
	@ParameterizedTest(name = "{0}: {4}")
	@CsvSource(delimiter = '|', textBlock = """
			SYN-0002 | PPO-HIGH-DED | 2026-01-01 |            | deductible fully met
			SYN-0003 | HMO-STANDARD | 2024-01-01 |            | out-of-pocket maximum met
			SYN-0005 | HMO-STANDARD | 2027-01-01 |            | coverage starting 2027-01-01
			SYN-0006 | PPO-LOW-DED  | 2026-01-01 | 2026-12-31 | coverage ending 2026-12-31
			SYN-0008 | PPO-LOW-DED  | 2025-01-01 |            | no Accumulator for the service year
			SYN-0009 | HMO-STANDARD | 2026-01-01 |            | plan excludes the requested procedure
			SYN-0010 | PPO-HIGH-DED | 2026-01-01 |            | plan has no Benefit for the requested procedure
			""")
	void seedMemberIsLoadedWithTheirCoverage(String memberId, String planId, LocalDate startDate,
			@Nullable LocalDate endDate, String edgeCase) {
		var expectedEndDate = (endDate == null) ? "null" : "\"" + endDate + "\"";
		coverageOf(memberId)
			.expectStatus().isOk()
			.expectBody().json("""
				{
				  "memberId": "%s",
				  "coverages": [{ "planId": "%s", "startDate": "%s", "endDate": %s }]
				}
				""".formatted(memberId, planId, startDate, expectedEndDate), JsonCompareMode.LENIENT);
	}

	/** RFC 9457 lets {@code type} be left out, meaning {@code about:blank}, and Spring leaves it out. */
	@Test
	void unknownMemberIsNotFoundAsProblemDetails() {
		coverageOf("SYN-9999")
			.expectStatus().isNotFound()
			.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
			.expectBody().json("""
				{
				  "title": "Not Found",
				  "status": 404,
				  "detail": "No member with ID SYN-9999.",
				  "instance": "/v1/members/SYN-9999/coverage"
				}
				""", JsonCompareMode.STRICT);
	}

	private ResponseSpec coverageOf(String memberId) {
		return client.get().uri("/v1/members/{memberId}/coverage", memberId).exchange();
	}

}
