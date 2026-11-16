package io.github.jconleyscales.eligibility.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.github.jconleyscales.eligibility.persistence.PlanType;
import io.github.jconleyscales.eligibility.rules.EligibilityRules.BenefitResult;
import io.github.jconleyscales.eligibility.rules.EligibilityRules.BenefitStatus;
import io.github.jconleyscales.eligibility.rules.EligibilityRules.CoverageResult;
import io.github.jconleyscales.eligibility.rules.EligibilityRules.EligibilityResult;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * The answer to an Eligibility check, with the request echoed back. The shape loosely follows FHIR
 * {@code CoverageEligibilityResponse}; ADR 0001 records the mapping.
 */
@Schema(description = "The answer to an Eligibility check, judged as of the Service date.")
record EligibilityCheckResponse(@Schema(description = "Echoed from the request.") String memberId,
		@Schema(description = "Echoed from the request.") String procedureCode,
		@Schema(description = "Echoed from the request.") LocalDate serviceDate, EligibilityCoverage coverage,
		@Schema(description = """
				The Plan's Benefit for the procedure. Null when coverage isn't active, so a Benefit the Member can't \
				use is never returned.""") @Nullable EligibilityBenefit benefit) {

	static EligibilityCheckResponse of(EligibilityCheckRequest request, EligibilityResult result) {
		var benefit = result.benefit();
		return new EligibilityCheckResponse(request.memberId(), request.procedureCode(), request.serviceDate(),
				EligibilityCoverage.of(result.coverage()), (benefit == null) ? null : EligibilityBenefit.of(benefit));
	}

	/**
	 * Whether coverage is active and, when it is, which Plan and plan year the answer comes from and what remains
	 * of the Plan's deductible and out-of-pocket maximum.
	 */
	@Schema(description = """
			Whether the Member has a Coverage active on the Service date and, when they do, which Plan and plan year \
			the answer comes from and what remains of the Plan's deductible and out-of-pocket maximum. Every field \
			but active is null when coverage isn't active.""")
	record EligibilityCoverage(
			@Schema(description = "Whether the Member has a Coverage active on the Service date.") boolean active,
			@Nullable String planId, @Nullable String planName, @Nullable PlanType planType,
			@Schema(description = "The calendar year of the Service date.") @Nullable Integer planYear,
			@Schema(description = """
					What remains of the Plan's deductible for this Coverage in the plan year, in dollars with two \
					decimals: the deductible minus what the Member has met, never below zero. With nothing recorded \
					for the plan year, the full deductible remains. A plan switch starts new totals at zero.""",
					minimum = "0") @Nullable BigDecimal deductibleRemaining,
			@Schema(description = """
					What remains of the Plan's out-of-pocket maximum for this Coverage in the plan year, in dollars \
					with two decimals: the maximum minus what the Member has paid, never below zero. With nothing \
					recorded for the plan year, the full maximum remains. A plan switch starts new totals at \
					zero.""", minimum = "0") @Nullable BigDecimal outOfPocketRemaining) {

		static EligibilityCoverage of(CoverageResult coverage) {
			return new EligibilityCoverage(coverage.active(), coverage.planId(), coverage.planName(),
					coverage.planType(), coverage.planYear(), coverage.deductibleRemaining(),
					coverage.outOfPocketRemaining());
		}

	}

	/**
	 * The Benefit status and, only when {@code COVERED}, its terms.
	 */
	@Schema(description = """
			The Plan's Benefit for the procedure. The terms (priorAuthRequired, deductibleApplies, copay, \
			coinsurancePercent) are set when status is COVERED and null otherwise.""")
	record EligibilityBenefit(@Schema(description = """
			COVERED: the Plan pays for the procedure. NOT_COVERED: the Plan has a Benefit saying it doesn't. \
			NO_BENEFIT_ON_FILE: the Plan has no Benefit for the procedure, which means unknown, not unpaid, so a \
			person decides. It is not a denial.""") BenefitStatus status,
			@Schema(description = "Whether prior authorization is required before the procedure.")
			@Nullable Boolean priorAuthRequired,
			@Schema(description = "Whether the deductible applies to the procedure.") @Nullable Boolean deductibleApplies,
			@Schema(description = "The copay in dollars, with two decimals.") @Nullable BigDecimal copay,
			@Schema(description = "The coinsurance, as a whole-number percentage.", minimum = "0", maximum = "100")
			@Nullable Integer coinsurancePercent) {

		static EligibilityBenefit of(BenefitResult benefit) {
			return new EligibilityBenefit(benefit.status(), benefit.priorAuthRequired(), benefit.deductibleApplies(),
					benefit.copay(), benefit.coinsurancePercent());
		}

	}

}
