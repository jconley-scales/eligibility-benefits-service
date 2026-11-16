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
import io.github.jconleyscales.eligibility.rules.EligibilityRules.PlanYearAccumulator;
import org.junit.jupiter.api.Test;

/**
 * The eligibility rules, with no Spring and no database. These stay beside the HTTP API tests because they are fast
 * and walk through the decisions one at a time. Every case here is also reachable through the HTTP API except two:
 * rounding a fraction of a cent, because the database stores money with two decimals, and recorded totals above the
 * Plan's limits, which the seed data doesn't have. Only this seam can show how the rules handle them.
 */
class EligibilityRulesTest {

	private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 3, 15);

	private static final ActiveCoverage PPO_COVERAGE = new ActiveCoverage("PPO-TEST", "Synthetic Test PPO",
			PlanType.PPO, new BigDecimal("1000.00"), new BigDecimal("5000.00"));

	private static final PlanBenefit COVERED_BENEFIT = new PlanBenefit(true, true, false, new BigDecimal("25.00"),
			20);

	@Test
	void withNoActiveCoverageTheMemberIsNotActiveAndGetsNoBenefitOrRemainingAmountsEvenIfOnFile() {
		var accumulator = new PlanYearAccumulator(new BigDecimal("100.00"), new BigDecimal("200.00"));

		var result = EligibilityRules.check(SERVICE_DATE, null, COVERED_BENEFIT, accumulator);

		assertThat(result.coverage()).isEqualTo(new CoverageResult(false, null, null, null, null, null, null));
		assertThat(result.benefit()).isNull();
	}

	@Test
	void activeCoverageNamesItsPlanAndThePlanYearIsTheCalendarYearOfTheServiceDate() {
		assertThat(EligibilityRules.check(LocalDate.of(2026, 12, 31), PPO_COVERAGE, COVERED_BENEFIT, null).coverage())
			.isEqualTo(new CoverageResult(true, "PPO-TEST", "Synthetic Test PPO", PlanType.PPO, 2026,
					new BigDecimal("1000.00"), new BigDecimal("5000.00")));
		assertThat(EligibilityRules.check(LocalDate.of(2027, 1, 1), PPO_COVERAGE, COVERED_BENEFIT, null).coverage())
			.isEqualTo(new CoverageResult(true, "PPO-TEST", "Synthetic Test PPO", PlanType.PPO, 2027,
					new BigDecimal("1000.00"), new BigDecimal("5000.00")));
	}

	@Test
	void aCoveredProcedureHasItsTerms() {
		var result = EligibilityRules.check(SERVICE_DATE, PPO_COVERAGE, COVERED_BENEFIT, null);

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

		var result = EligibilityRules.check(SERVICE_DATE, PPO_COVERAGE, excluded, null);

		assertThat(result.benefit()).isEqualTo(new BenefitResult(BenefitStatus.NOT_COVERED, null, null, null, null));
	}

	/** Unknown, not unpaid: a missing Benefit is never reported as not covered. */
	@Test
	void aProcedureWithNoBenefitOnFileIsReportedAsSuchNotAsNotCovered() {
		var result = EligibilityRules.check(SERVICE_DATE, PPO_COVERAGE, null, null);

		assertThat(result.benefit())
			.isEqualTo(new BenefitResult(BenefitStatus.NO_BENEFIT_ON_FILE, null, null, null, null));
	}

	@Test
	void remainingAmountsAreThePlanLimitsMinusWhatTheAccumulatorSaysHasBeenMet() {
		var accumulator = new PlanYearAccumulator(new BigDecimal("250.00"), new BigDecimal("1250.50"));

		var coverage = EligibilityRules.check(SERVICE_DATE, PPO_COVERAGE, COVERED_BENEFIT, accumulator).coverage();

		assertThat(coverage.deductibleRemaining()).isEqualTo(new BigDecimal("750.00"));
		assertThat(coverage.outOfPocketRemaining()).isEqualTo(new BigDecimal("3749.50"));
	}

	/** Nothing recorded for the plan year means nothing has been met yet. */
	@Test
	void withNoAccumulatorTheFullDeductibleAndOutOfPocketMaximumRemain() {
		var coverage = EligibilityRules.check(SERVICE_DATE, PPO_COVERAGE, COVERED_BENEFIT, null).coverage();

		assertThat(coverage.deductibleRemaining()).isEqualTo(new BigDecimal("1000.00"));
		assertThat(coverage.outOfPocketRemaining()).isEqualTo(new BigDecimal("5000.00"));
	}

	/** {@link BigDecimal#equals} compares the scale too, so 500 and 500.00 are different here. */
	@Test
	void remainingAmountsAreDollarsWithTwoDecimals() {
		var wholeDollarPlan = new ActiveCoverage("PPO-TEST", "Synthetic Test PPO", PlanType.PPO,
				new BigDecimal("500"), new BigDecimal("3000"));
		var accumulator = new PlanYearAccumulator(new BigDecimal("0"), new BigDecimal("12.5"));

		var coverage = EligibilityRules.check(SERVICE_DATE, wholeDollarPlan, COVERED_BENEFIT, accumulator).coverage();

		assertThat(coverage.deductibleRemaining()).isEqualTo(new BigDecimal("500.00"));
		assertThat(coverage.outOfPocketRemaining()).isEqualTo(new BigDecimal("2987.50"));
	}

	/**
	 * The database can't stop recorded totals passing the Plan's limits (they live in different tables), and the
	 * seed data has none that do, so only this seam shows the rules never report negative money.
	 */
	@Test
	void remainingAmountsNeverGoBelowZeroWhenRecordedTotalsExceedThePlanLimits() {
		var overLimits = new PlanYearAccumulator(new BigDecimal("1000.01"), new BigDecimal("6250.00"));

		var coverage = EligibilityRules.check(SERVICE_DATE, PPO_COVERAGE, COVERED_BENEFIT, overLimits).coverage();

		assertThat(coverage.deductibleRemaining()).isEqualTo(new BigDecimal("0.00"));
		assertThat(coverage.outOfPocketRemaining()).isEqualTo(new BigDecimal("0.00"));
	}

	private static BigDecimal copayOf(String copay) {
		var benefit = new PlanBenefit(true, false, false, new BigDecimal(copay), 0);
		return EligibilityRules.check(SERVICE_DATE, PPO_COVERAGE, benefit, null).benefit().copay();
	}

}
