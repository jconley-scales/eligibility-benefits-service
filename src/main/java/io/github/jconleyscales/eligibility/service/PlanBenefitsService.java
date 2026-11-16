package io.github.jconleyscales.eligibility.service;

import java.util.List;

import io.github.jconleyscales.eligibility.persistence.Benefit;
import io.github.jconleyscales.eligibility.persistence.BenefitRepository;
import io.github.jconleyscales.eligibility.persistence.Plan;
import io.github.jconleyscales.eligibility.persistence.PlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Looks up which procedures a Plan pays for, and on what terms.
 */
@Service
public class PlanBenefitsService {

	private final PlanRepository plans;

	private final BenefitRepository benefits;

	PlanBenefitsService(PlanRepository plans, BenefitRepository benefits) {
		this.plans = plans;
		this.benefits = benefits;
	}

	/**
	 * @return the Plan and its Benefits, ordered by procedure code
	 * @throws PlanNotFoundException if no Plan has this ID
	 */
	@Transactional(readOnly = true)
	public PlanBenefits planBenefits(String planId) {
		var plan = plans.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));
		return new PlanBenefits(plan, benefits.findByPlanIdOrderByProcedureCodeAsc(planId));
	}

	/**
	 * A Plan and its Benefits. A procedure with no Benefit here has no benefit on file under the Plan.
	 */
	public record PlanBenefits(Plan plan, List<Benefit> benefits) {
	}

}
