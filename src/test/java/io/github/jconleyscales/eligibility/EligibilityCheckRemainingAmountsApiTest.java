package io.github.jconleyscales.eligibility;

import static io.github.jconleyscales.eligibility.RawJson.valuesOf;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

/**
 * {@code POST /v1/eligibility/check}: what's left of the Plan's deductible and out-of-pocket maximum for the
 * active Coverage in the plan year containing the Service date. Each test uses a seed Member's Accumulator edge
 * case and an explicit Service date, never today.
 */
@IntegrationTest
class EligibilityCheckRemainingAmountsApiTest {

	@Autowired
	RestTestClient client;

	/** PPO-LOW-DED: $500.00 deductible, $3,000.00 out-of-pocket maximum; $187.50 and $412.75 met in 2026. */
	@Test
	void partOfTheDeductibleMetLeavesThePlanLimitsMinusWhatHasBeenMet() {
		expectRemaining("SYN-0001", "2026-03-15", "PPO-LOW-DED", "312.50", "2587.25");
	}

	/** PPO-HIGH-DED: $3,000.00 deductible, $7,000.00 out-of-pocket maximum; $3,000.00 and $3,600.00 met in 2026. */
	@Test
	void deductibleFullyMetLeavesNoDeductibleRemaining() {
		expectRemaining("SYN-0002", "2026-03-15", "PPO-HIGH-DED", "0.00", "3400.00");
	}

	/** HMO-STANDARD: $250.00 deductible, $4,000.00 out-of-pocket maximum; both fully met in 2026. */
	@Test
	void outOfPocketMaximumMetLeavesNoOutOfPocketRemaining() {
		expectRemaining("SYN-0003", "2026-03-15", "HMO-STANDARD", "0.00", "0.00");
	}

	/**
	 * PPO-HIGH-DED ($3,000.00 / $7,000.00, with $1,200.00 and $1,500.00 met) ends 2026-06-30, and HMO-STANDARD
	 * ($250.00 / $4,000.00) starts 2026-07-01 with nothing recorded. Totals belong to one Coverage, so the first
	 * Plan's spending doesn't carry over.
	 */
	@Test
	void planSwitchStartsTheNewPlanWithItsFullDeductibleAndOutOfPocketMaximumRemaining() {
		expectRemaining("SYN-0007", "2026-06-30", "PPO-HIGH-DED", "1800.00", "5500.00");
		expectRemaining("SYN-0007", "2026-07-01", "HMO-STANDARD", "250.00", "4000.00");
	}

	/**
	 * PPO-LOW-DED: $500.00 deductible, $3,000.00 out-of-pocket maximum. The ongoing Coverage has totals for 2025
	 * ($500.00 and $1,250.00 met) and none for 2026. Nothing recorded means nothing met, and another year's totals
	 * don't count.
	 */
	@Test
	void noAccumulatorForThePlanYearLeavesTheFullDeductibleAndOutOfPocketMaximumRemaining() {
		expectRemaining("SYN-0008", "2026-03-15", "PPO-LOW-DED", "500.00", "3000.00");
		expectRemaining("SYN-0008", "2025-03-15", "PPO-LOW-DED", "0.00", "1750.00");
	}

	/**
	 * JSON comparison treats 0.00 and 0 as the same number, so this reads the raw body to check the units the API
	 * promises: money has exactly two decimals, even when they are zeros.
	 */
	@Test
	void remainingAmountsAreDollarsWithTwoDecimals() {
		var deductibleMetBody = rawBody(check("SYN-0002", "2026-03-15"));
		assertThat(valuesOf(deductibleMetBody, "deductibleRemaining")).containsExactly("0.00");
		assertThat(valuesOf(deductibleMetBody, "outOfPocketRemaining")).containsExactly("3400.00");

		var nothingMetBody = rawBody(check("SYN-0008", "2026-03-15"));
		assertThat(valuesOf(nothingMetBody, "deductibleRemaining")).containsExactly("500.00");
		assertThat(valuesOf(nothingMetBody, "outOfPocketRemaining")).containsExactly("3000.00");
	}

	private void expectRemaining(String memberId, String serviceDate, String planId, String deductibleRemaining,
			String outOfPocketRemaining) {
		check(memberId, serviceDate)
			.expectStatus().isOk()
			.expectBody().json("""
				{
				  "coverage": {
				    "active": true,
				    "planId": "%s",
				    "deductibleRemaining": %s,
				    "outOfPocketRemaining": %s
				  }
				}
				""".formatted(planId, deductibleRemaining, outOfPocketRemaining), JsonCompareMode.LENIENT);
	}

	/** 29881 has a covered Benefit on every seed Plan; the remaining amounts don't depend on the procedure. */
	private ResponseSpec check(String memberId, String serviceDate) {
		return client.post().uri("/v1/eligibility/check")
			.contentType(MediaType.APPLICATION_JSON)
			.body("""
				{ "memberId": "%s", "procedureCode": "29881", "serviceDate": "%s" }
				""".formatted(memberId, serviceDate))
			.exchange();
	}

	private static String rawBody(ResponseSpec response) {
		return response.expectStatus().isOk().expectBody(String.class).returnResult().getResponseBody();
	}

}
