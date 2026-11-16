package io.github.jconleyscales.eligibility;

import static io.github.jconleyscales.eligibility.RawJson.valuesOf;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

/**
 * {@code POST /v1/eligibility/check}: one answer for one Member, one procedure, and one Service date, judged as of
 * the Service date. Each test uses a seed Member's edge case and an explicit Service date, never today.
 */
@IntegrationTest
class EligibilityCheckApiTest {

	@Autowired
	RestTestClient client;

	@Test
	void ongoingCoverageWithNoEndDateIsActiveAndACoveredBenefitHasItsTerms() {
		check("SYN-0001", "29881", "2026-03-15")
			.expectStatus().isOk()
			.expectHeader().contentType(MediaType.APPLICATION_JSON)
			.expectBody().json("""
				{
				  "memberId": "SYN-0001",
				  "procedureCode": "29881",
				  "serviceDate": "2026-03-15",
				  "coverage": {
				    "active": true,
				    "planId": "PPO-LOW-DED",
				    "planName": "Synthetic Low-Deductible PPO",
				    "planType": "PPO",
				    "planYear": 2026
				  },
				  "benefit": {
				    "status": "COVERED",
				    "priorAuthRequired": true,
				    "deductibleApplies": true,
				    "copay": 0.00,
				    "coinsurancePercent": 20
				  }
				}
				""", JsonCompareMode.STRICT);
	}

	/** The end date is the last covered day. */
	@Test
	void coverageEndingDecember31IsActiveOnItsEndDateAndInactiveTheDayAfter() {
		expectActiveOn("SYN-0006", "2026-12-31", "PPO-LOW-DED", 2026);
		expectNotActiveOn("SYN-0006", "2027-01-01");
	}

	@Test
	void coverageStartingJanuary1IsInactiveTheDayBeforeAndActiveFromItsStartDate() {
		expectNotActiveOn("SYN-0005", "2026-12-31");
		expectActiveOn("SYN-0005", "2027-01-01", "HMO-STANDARD", 2027);
	}

	/**
	 * PPO-HIGH-DED ends 2026-06-30 and HMO-STANDARD starts 2026-07-01. The two Plans' Benefits for 72148 differ, so
	 * the Benefit shows which Plan answered too.
	 */
	@Test
	void planSwitchIsAnsweredFromTheFirstPlanBeforeJuly1AndFromTheSecondPlanFromJuly1() {
		check("SYN-0007", "72148", "2026-06-30")
			.expectStatus().isOk()
			.expectBody().json("""
				{
				  "coverage": {
				    "active": true,
				    "planId": "PPO-HIGH-DED",
				    "planName": "Synthetic High-Deductible PPO",
				    "planType": "PPO",
				    "planYear": 2026
				  },
				  "benefit": {
				    "status": "COVERED",
				    "priorAuthRequired": true,
				    "deductibleApplies": true,
				    "copay": 0.00,
				    "coinsurancePercent": 30
				  }
				}
				""", JsonCompareMode.LENIENT);

		check("SYN-0007", "72148", "2026-07-01")
			.expectStatus().isOk()
			.expectBody().json("""
				{
				  "coverage": {
				    "active": true,
				    "planId": "HMO-STANDARD",
				    "planName": "Synthetic Standard HMO",
				    "planType": "HMO",
				    "planYear": 2026
				  },
				  "benefit": {
				    "status": "COVERED",
				    "priorAuthRequired": true,
				    "deductibleApplies": false,
				    "copay": 75.00,
				    "coinsurancePercent": 0
				  }
				}
				""", JsonCompareMode.LENIENT);
	}

	/** The Coverage ended 2025-12-31, and a Coverage that has ended is never treated as current. */
	@Test
	void lapsedCoverageIsNotActiveAndHasNoBenefit() {
		check("SYN-0004", "29881", "2026-03-15")
			.expectStatus().isOk()
			.expectBody().json("""
				{
				  "memberId": "SYN-0004",
				  "procedureCode": "29881",
				  "serviceDate": "2026-03-15",
				  "coverage": {
				    "active": false,
				    "planId": null,
				    "planName": null,
				    "planType": null,
				    "planYear": null
				  },
				  "benefit": null
				}
				""", JsonCompareMode.STRICT);
	}

	/** HMO-STANDARD has a Benefit saying it doesn't pay for 95810, so there are no terms. */
	@Test
	void excludingPlanReturnsNotCoveredWithNoTerms() {
		check("SYN-0009", "95810", "2026-03-15")
			.expectStatus().isOk()
			.expectBody().json("""
				{
				  "coverage": { "active": true, "planId": "HMO-STANDARD" },
				  "benefit": {
				    "status": "NOT_COVERED",
				    "priorAuthRequired": null,
				    "deductibleApplies": null,
				    "copay": null,
				    "coinsurancePercent": null
				  }
				}
				""", JsonCompareMode.LENIENT);
	}

	/** PPO-HIGH-DED has no Benefit for 97110: unknown, not unpaid, so it is never reported as NOT_COVERED. */
	@Test
	void planWithNoBenefitForTheProcedureHasNoBenefitOnFile() {
		check("SYN-0010", "97110", "2026-03-15")
			.expectStatus().isOk()
			.expectBody().json("""
				{
				  "coverage": { "active": true, "planId": "PPO-HIGH-DED" },
				  "benefit": {
				    "status": "NO_BENEFIT_ON_FILE",
				    "priorAuthRequired": null,
				    "deductibleApplies": null,
				    "copay": null,
				    "coinsurancePercent": null
				  }
				}
				""", JsonCompareMode.LENIENT);
	}

	/**
	 * Missing Benefits are an answer, not a failure. Only the format of a code is checked, never whether it is in the
	 * real CPT or HCPCS code sets; {@link EligibilityCheckValidationApiTest} covers malformed codes.
	 */
	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "99213", "0042T", "J3490" })
	void wellFormedCodeThePlanDoesNotListHasNoBenefitOnFileRatherThanAnError(String procedureCode) {
		check("SYN-0001", procedureCode, "2026-03-15")
			.expectStatus().isOk()
			.expectBody().json("""
				{
				  "procedureCode": "%s",
				  "coverage": { "active": true, "planId": "PPO-LOW-DED" },
				  "benefit": { "status": "NO_BENEFIT_ON_FILE" }
				}
				""".formatted(procedureCode), JsonCompareMode.LENIENT);
	}

	/**
	 * JSON comparison treats 30.00 and 30 as the same number, so this reads the raw body to check the units the
	 * API promises: money has exactly two decimals, even when they are zeros, and coinsurance is a whole number.
	 */
	@Test
	void moneyIsDollarsWithTwoDecimalsAndCoinsuranceIsAWholeNumber() {
		var copayBody = rawBody(check("SYN-0001", "97110", "2026-03-15"));
		assertThat(valuesOf(copayBody, "copay")).containsExactly("30.00");
		assertThat(valuesOf(copayBody, "coinsurancePercent")).containsExactly("0");

		var coinsuranceBody = rawBody(check("SYN-0001", "29881", "2026-03-15"));
		assertThat(valuesOf(coinsuranceBody, "copay")).containsExactly("0.00");
		assertThat(valuesOf(coinsuranceBody, "coinsurancePercent")).containsExactly("20");
	}

	/**
	 * A data error, distinct from a Member whose coverage isn't active. RFC 9457 lets {@code type} be left out,
	 * meaning {@code about:blank}, and Spring leaves it out.
	 */
	@Test
	void unknownMemberIsNotFoundAsProblemDetailsNotCoverageNotActive() {
		check("SYN-9999", "29881", "2026-03-15")
			.expectStatus().isNotFound()
			.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
			.expectBody().json("""
				{
				  "title": "Not Found",
				  "status": 404,
				  "detail": "No member with ID SYN-9999.",
				  "instance": "/v1/eligibility/check"
				}
				""", JsonCompareMode.STRICT);
	}

	private ResponseSpec check(String memberId, String procedureCode, String serviceDate) {
		return client.post().uri("/v1/eligibility/check")
			.contentType(MediaType.APPLICATION_JSON)
			.body("""
				{ "memberId": "%s", "procedureCode": "%s", "serviceDate": "%s" }
				""".formatted(memberId, procedureCode, serviceDate))
			.exchange();
	}

	/** 29881 has a covered Benefit on every seed Plan, so an active Coverage always has a COVERED Benefit. */
	private void expectActiveOn(String memberId, String serviceDate, String planId, int planYear) {
		check(memberId, "29881", serviceDate)
			.expectStatus().isOk()
			.expectBody().json("""
				{
				  "serviceDate": "%s",
				  "coverage": { "active": true, "planId": "%s", "planYear": %d },
				  "benefit": { "status": "COVERED" }
				}
				""".formatted(serviceDate, planId, planYear), JsonCompareMode.LENIENT);
	}

	private void expectNotActiveOn(String memberId, String serviceDate) {
		check(memberId, "29881", serviceDate)
			.expectStatus().isOk()
			.expectBody().json("""
				{
				  "serviceDate": "%s",
				  "coverage": { "active": false, "planId": null, "planYear": null },
				  "benefit": null
				}
				""".formatted(serviceDate), JsonCompareMode.LENIENT);
	}

	private static String rawBody(ResponseSpec response) {
		return response.expectStatus().isOk().expectBody(String.class).returnResult().getResponseBody();
	}

}
