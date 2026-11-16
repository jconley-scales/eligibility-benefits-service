package io.github.jconleyscales.eligibility.web;

import java.math.BigDecimal;
import java.util.List;

import io.github.jconleyscales.eligibility.persistence.Benefit;
import io.github.jconleyscales.eligibility.persistence.PlanType;
import io.github.jconleyscales.eligibility.service.PlanBenefitsService.PlanBenefits;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * A Plan's details and its Benefits, ordered by procedure code.
 */
record PlanBenefitsResponse(String planId, String planName, PlanType planType,
		@Schema(description = "In dollars, with two decimals.") BigDecimal deductible,
		@Schema(description = "In dollars, with two decimals.") BigDecimal outOfPocketMaximum,
		@Schema(description = """
				One Benefit per procedure code. A procedure that isn't listed has no benefit on file under this \
				Plan, which means unknown, not unpaid.""") List<BenefitSummary> benefits) {

	static PlanBenefitsResponse of(PlanBenefits planBenefits) {
		var plan = planBenefits.plan();
		return new PlanBenefitsResponse(plan.getId(), plan.getName(), plan.getPlanType(), plan.getDeductible(),
				plan.getOutOfPocketMaximum(), planBenefits.benefits().stream().map(BenefitSummary::of).toList());
	}

	/**
	 * A Plan's Benefit for one procedure. When {@code covered} is false the Plan excludes the procedure, and
	 * every term after it is null.
	 */
	@Schema(description = """
			A Plan's Benefit for one procedure. When covered is false the Plan excludes the procedure, and the \
			terms (priorAuthRequired, deductibleApplies, copay, coinsurancePercent) are null.""")
	record BenefitSummary(@Schema(description = "A CPT or HCPCS code.") String procedureCode,
			@Schema(description = "Whether the Plan pays for the procedure.") boolean covered,
			@Schema(description = "Whether prior authorization is required before the procedure.")
			@Nullable Boolean priorAuthRequired,
			@Schema(description = "Whether the deductible applies to the procedure.") @Nullable Boolean deductibleApplies,
			@Schema(description = "The copay in dollars, with two decimals.") @Nullable BigDecimal copay,
			@Schema(description = "The coinsurance, as a whole-number percentage.", minimum = "0", maximum = "100")
			@Nullable Integer coinsurancePercent) {

		static BenefitSummary of(Benefit benefit) {
			return new BenefitSummary(benefit.getProcedureCode(), benefit.isCovered(), benefit.getPriorAuthRequired(),
					benefit.getDeductibleApplies(), benefit.getCopay(), benefit.getCoinsurancePercent());
		}

	}

}
