package io.github.jconleyscales.eligibility.rules;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import io.github.jconleyscales.eligibility.persistence.PlanType;
import org.jspecify.annotations.Nullable;

/**
 * Every decision an Eligibility check makes, as one pure function with no I/O and no framework. The caller loads
 * the data; this decides what it means:
 * <ul>
 * <li>Coverage is active when the Member has a Coverage on the Service date. Finding it is the caller's single
 * query, so the rules get the active Coverage or none.</li>
 * <li>The plan year is the calendar year of the Service date.</li>
 * <li>With no active Coverage there is no Benefit, even if the Plan has one: a Member can't use it.</li>
 * <li>The Benefit status is {@code COVERED} when the Plan pays for the procedure, {@code NOT_COVERED} when the
 * Plan has a Benefit saying it doesn't, and {@code NO_BENEFIT_ON_FILE} when the Plan has no Benefit for it. No
 * benefit on file means unknown, not unpaid, so it is never reported as not covered.</li>
 * <li>Only a {@code COVERED} Benefit has terms: prior authorization, deductible, copay, and coinsurance.</li>
 * <li>Money is dollars with two decimals, rounding half up to the cent.</li>
 * </ul>
 * The inputs and results are plain records rather than the persistence entities, so nothing here can trigger a
 * lazy database load and the rules can be tested without Spring or a database.
 */
public final class EligibilityRules {

	private EligibilityRules() {
	}

	/**
	 * @param serviceDate the date the procedure is or was performed; the answer is judged as of this date
	 * @param coverage the Member's Coverage active on the Service date, or {@code null} when they have none
	 * @param benefit the Plan's Benefit for the procedure, or {@code null} when the Plan has no benefit on file for
	 * it
	 */
	public static EligibilityResult check(LocalDate serviceDate, @Nullable ActiveCoverage coverage,
			@Nullable PlanBenefit benefit) {
		if (coverage == null) {
			return new EligibilityResult(new CoverageResult(false, null, null, null, null), null);
		}
		return new EligibilityResult(
				new CoverageResult(true, coverage.planId(), coverage.planName(), coverage.planType(),
						serviceDate.getYear()),
				benefitResult(benefit));
	}

	private static BenefitResult benefitResult(@Nullable PlanBenefit benefit) {
		if (benefit == null) {
			return new BenefitResult(BenefitStatus.NO_BENEFIT_ON_FILE, null, null, null, null);
		}
		if (!benefit.covered()) {
			return new BenefitResult(BenefitStatus.NOT_COVERED, null, null, null, null);
		}
		return new BenefitResult(BenefitStatus.COVERED, benefit.priorAuthRequired(), benefit.deductibleApplies(),
				dollars(benefit.copay()), benefit.coinsurancePercent());
	}

	private static @Nullable BigDecimal dollars(@Nullable BigDecimal amount) {
		return (amount == null) ? null : amount.setScale(2, RoundingMode.HALF_UP);
	}

	/**
	 * The Member's Coverage active on the Service date, described by its Plan.
	 */
	public record ActiveCoverage(String planId, String planName, PlanType planType) {
	}

	/**
	 * The Plan's Benefit for the requested procedure. The terms are null when the Plan doesn't pay for it.
	 */
	public record PlanBenefit(boolean covered, @Nullable Boolean priorAuthRequired,
			@Nullable Boolean deductibleApplies, @Nullable BigDecimal copay, @Nullable Integer coinsurancePercent) {
	}

	/**
	 * The answer to an Eligibility check. {@code benefit} is null exactly when coverage isn't active.
	 */
	public record EligibilityResult(CoverageResult coverage, @Nullable BenefitResult benefit) {
	}

	/**
	 * Whether coverage is active on the Service date and, when it is, which Plan and plan year the answer comes
	 * from. Everything but {@code active} is null when coverage isn't active.
	 */
	public record CoverageResult(boolean active, @Nullable String planId, @Nullable String planName,
			@Nullable PlanType planType, @Nullable Integer planYear) {
	}

	/**
	 * The Benefit status and, only when {@code COVERED}, its terms. The copay is in dollars with two decimals and
	 * the coinsurance is a whole-number percentage.
	 */
	public record BenefitResult(BenefitStatus status, @Nullable Boolean priorAuthRequired,
			@Nullable Boolean deductibleApplies, @Nullable BigDecimal copay, @Nullable Integer coinsurancePercent) {
	}

	/**
	 * Whether the Plan pays for the procedure. {@code NOT_COVERED} and {@code NO_BENEFIT_ON_FILE} are never
	 * confused: the first is the Plan's answer, the second means there is no answer on file and a person decides.
	 */
	public enum BenefitStatus {
		COVERED, NOT_COVERED, NO_BENEFIT_ON_FILE
	}

}
