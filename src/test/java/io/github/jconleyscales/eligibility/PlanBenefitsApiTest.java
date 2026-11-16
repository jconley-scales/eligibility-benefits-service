package io.github.jconleyscales.eligibility;

import static io.github.jconleyscales.eligibility.RawJson.valuesOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

/**
 * {@code GET /v1/plans/{planId}/benefits}: a Plan's details and its Benefits, read from the seed data.
 */
@IntegrationTest
class PlanBenefitsApiTest {

	@Autowired
	RestTestClient client;

	@Test
	void planHasItsDetailsAndOneBenefitPerProcedureOrderedByProcedureCode() {
		benefitsOf("PPO-LOW-DED")
			.expectStatus().isOk()
			.expectHeader().contentType(MediaType.APPLICATION_JSON)
			.expectBody().json("""
				{
				  "planId": "PPO-LOW-DED",
				  "planName": "Synthetic Low-Deductible PPO",
				  "planType": "PPO",
				  "deductible": 500.00,
				  "outOfPocketMaximum": 3000.00,
				  "benefits": [
				    { "procedureCode": "29881", "covered": true, "priorAuthRequired": true,
				      "deductibleApplies": true, "copay": 0.00, "coinsurancePercent": 20 },
				    { "procedureCode": "72148", "covered": true, "priorAuthRequired": false,
				      "deductibleApplies": true, "copay": 0.00, "coinsurancePercent": 20 },
				    { "procedureCode": "95810", "covered": true, "priorAuthRequired": true,
				      "deductibleApplies": true, "copay": 0.00, "coinsurancePercent": 20 },
				    { "procedureCode": "97110", "covered": true, "priorAuthRequired": false,
				      "deductibleApplies": false, "copay": 30.00, "coinsurancePercent": 0 },
				    { "procedureCode": "E0601", "covered": true, "priorAuthRequired": true,
				      "deductibleApplies": true, "copay": 0.00, "coinsurancePercent": 20 }
				  ]
				}
				""", JsonCompareMode.STRICT);
	}

	/**
	 * The Plan for the "plan excludes the requested procedure" Member, SYN-0009. An excluded procedure has no
	 * terms: a copay of 0.00 would read as free.
	 */
	@Test
	void excludingPlanShowsTheProcedureAsNotCoveredWithNoTerms() {
		benefitsOf("HMO-STANDARD")
			.expectStatus().isOk()
			.expectBody().json("""
				{
				  "planId": "HMO-STANDARD",
				  "planName": "Synthetic Standard HMO",
				  "planType": "HMO",
				  "deductible": 250.00,
				  "outOfPocketMaximum": 4000.00,
				  "benefits": [
				    { "procedureCode": "29881", "covered": true, "priorAuthRequired": true,
				      "deductibleApplies": false, "copay": 150.00, "coinsurancePercent": 0 },
				    { "procedureCode": "72148", "covered": true, "priorAuthRequired": true,
				      "deductibleApplies": false, "copay": 75.00, "coinsurancePercent": 0 },
				    { "procedureCode": "95810", "covered": false, "priorAuthRequired": null,
				      "deductibleApplies": null, "copay": null, "coinsurancePercent": null },
				    { "procedureCode": "97110", "covered": true, "priorAuthRequired": false,
				      "deductibleApplies": false, "copay": 25.00, "coinsurancePercent": 0 },
				    { "procedureCode": "E0601", "covered": true, "priorAuthRequired": true,
				      "deductibleApplies": true, "copay": 0.00, "coinsurancePercent": 10 }
				  ]
				}
				""", JsonCompareMode.STRICT);
	}

	/**
	 * The Plan for the "plan with no Benefit for the requested procedure" Member, SYN-0010: 97110 isn't listed,
	 * so the Plan has no benefit on file for it.
	 */
	@Test
	void planWithNoBenefitForAProcedureLeavesItOut() {
		benefitsOf("PPO-HIGH-DED")
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.planId").isEqualTo("PPO-HIGH-DED")
			.jsonPath("$.benefits[*].procedureCode").isEqualTo(List.of("29881", "72148", "95810", "E0601"))
			.jsonPath("$.benefits[*].covered").isEqualTo(List.of(true, true, true, true));
	}

	/**
	 * JSON comparison treats 30.00 and 30 as the same number, so this reads the raw body to check the units the
	 * API promises: every amount of money has exactly two decimals, even when they are zeros, and coinsurance is
	 * a whole number.
	 */
	@Test
	void moneyIsDollarsWithTwoDecimalsAndCoinsuranceIsAWholeNumber() {
		var body = benefitsOf("PPO-LOW-DED")
			.expectStatus().isOk()
			.expectBody(String.class).returnResult().getResponseBody();

		assertThat(valuesOf(body, "deductible")).containsExactly("500.00");
		assertThat(valuesOf(body, "outOfPocketMaximum")).containsExactly("3000.00");
		assertThat(valuesOf(body, "copay")).containsExactly("0.00", "0.00", "0.00", "30.00", "0.00");
		assertThat(valuesOf(body, "coinsurancePercent")).containsExactly("20", "20", "20", "0", "20");
	}

	/** RFC 9457 lets {@code type} be left out, meaning {@code about:blank}, and Spring leaves it out. */
	@Test
	void unknownPlanIsNotFoundAsProblemDetails() {
		benefitsOf("NO-SUCH-PLAN")
			.expectStatus().isNotFound()
			.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
			.expectBody().json("""
				{
				  "title": "Not Found",
				  "status": 404,
				  "detail": "No plan with ID NO-SUCH-PLAN.",
				  "instance": "/v1/plans/NO-SUCH-PLAN/benefits"
				}
				""", JsonCompareMode.STRICT);
	}

	private ResponseSpec benefitsOf(String planId) {
		return client.get().uri("/v1/plans/{planId}/benefits", planId).exchange();
	}

}
