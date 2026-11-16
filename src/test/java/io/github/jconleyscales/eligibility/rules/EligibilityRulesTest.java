package io.github.jconleyscales.eligibility.rules;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.github.jconleyscales.eligibility.persistence.PlanType;
import io.github.jconleyscales.eligibility.rules.EligibilityRules.ActiveCoverage;
import io.github.jconleyscales.eligibility.rules.EligibilityRules.BenefitResult;
import io.github.jconleyscales.eligibility.rules.EligibilityRules.BenefitStatus;
import io.github.jconleyscales.eligibility.rules.EligibilityRules.CoverageResult;
import io.github.jconleyscales.eligibility.rules.EligibilityRules.PlanBenefit;
import org.junit.jupiter.api.Test;

/**
 * The eligibility rules, with no Spring and no database. These stay beside the HTTP API tests because they are fast
 * and walk through the decisions one at a time. Every case here is also reachable through the HTTP API except
 * rounding a fraction of a cent: the database stores money with two decimals, so only this seam can show the rules
 * would round it.
 */
class EligibilityRulesTest {

	private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 3, 15);

	private static final ActiveCoverage PPO_COVERAGE = new ActiveCoverage("PPO-TEST", "Synthetic Test PPO",
			PlanType.PPO);

	private static final PlanBenefit COVERED_BENEFIT = new PlanBenefit(true, true, false, new BigDecimal("25.00"),
			20);

	@Test
	void withNoActiveCoverageTheMemberIsNotActiveAndGetsNoBenefitEvenIfOneIsOnFile() {
		var result = EligibilityRules.check(SERVICE_DATE, null, COVERED_BENEFIT);

		assertThat(result.coverage()).isEqualTo(new CoverageResult(false, null, null, null, null));
		assertThat(result.benefit()).isNull();
	}

	@Test
	void activeCoverageNamesItsPlanAndThePlanYearIsTheCalendarYearOfTheServiceDate() {
		assertThat(EligibilityRules.check(LocalDate.of(2026, 12, 31), PPO_COVERAGE, COVERED_BENEFIT).coverage())
			.isEqualTo(new CoverageResult(true, "PPO-TEST", "Synthetic Test PPO", PlanType.PPO, 2026));
		assertThat(EligibilityRules.check(LocalDate.of(2027, 1, 1), PPO_COVERAGE, COVERED_BENEFIT).coverage())
			.isEqualTo(new CoverageResult(true, "PPO-TEST", "Synthetic Test PPO", PlanType.PPO, 2027));
	}

	@Test
	void aCoveredProcedureHasItsTerms() {
		var result = EligibilityRules.check(SERVICE_DATE, PPO_COVERAGE, COVERED_BENEFIT);

		assertThat(result.benefit())
			.isEqualTo(new BenefitResult(BenefitStatus.COVERED, true, false, new BigDecimal("25.00"), 20));
	}

	/** {@link BigDecimal#equals} compares the scale too, so 30 and 30.00 are different here. */
	@Test
	void moneyIsDollarsWithTwoDecimals() {
		assertThat(copayOf("30")).isEqualTo(new BigDecimal("30.00"));
		assertThat(copayOf("0")).isEqualTo(new BigDecimal("0.00"));
		assertThat(copayOf("12.5")).isEqualTo(new BigDecimal("12.50"));
	}

	@Test
	void moneyWithFractionsOfACentRoundsHalfUpToTheCent() {
		assertThat(copayOf("12.345")).isEqualTo(new BigDecimal("12.35"));
		assertThat(copayOf("12.344")).isEqualTo(new BigDecimal("12.34"));
	}

	@Test
	void aProcedureThePlanExcludesIsNotCoveredAndHasNoTerms() {
		var excluded = new PlanBenefit(false, null, null, null, null);

		var result = EligibilityRules.check(SERVICE_DATE, PPO_COVERAGE, excluded);

		assertThat(result.benefit()).isEqualTo(new BenefitResult(BenefitStatus.NOT_COVERED, null, null, null, null));
	}

	/** Unknown, not unpaid: a missing Benefit is never reported as not covered. */
	@Test
	void aProcedureWithNoBenefitOnFileIsReportedAsSuchNotAsNotCovered() {
		var result = EligibilityRules.check(SERVICE_DATE, PPO_COVERAGE, null);

		assertThat(result.benefit())
			.isEqualTo(new BenefitResult(BenefitStatus.NO_BENEFIT_ON_FILE, null, null, null, null));
	}

	private static BigDecimal copayOf(String copay) {
		var benefit = new PlanBenefit(true, false, false, new BigDecimal(copay), 0);
		return EligibilityRules.check(SERVICE_DATE, PPO_COVERAGE, benefit).benefit().copay();
	}

}
