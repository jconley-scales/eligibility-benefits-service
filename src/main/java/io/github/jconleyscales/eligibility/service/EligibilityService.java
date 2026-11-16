package io.github.jconleyscales.eligibility.service;

import java.time.LocalDate;

import io.github.jconleyscales.eligibility.persistence.Benefit;
import io.github.jconleyscales.eligibility.persistence.BenefitRepository;
import io.github.jconleyscales.eligibility.persistence.Coverage;
import io.github.jconleyscales.eligibility.persistence.CoverageRepository;
import io.github.jconleyscales.eligibility.persistence.MemberRepository;
import io.github.jconleyscales.eligibility.rules.EligibilityRules;
import io.github.jconleyscales.eligibility.rules.EligibilityRules.ActiveCoverage;
import io.github.jconleyscales.eligibility.rules.EligibilityRules.EligibilityResult;
import io.github.jconleyscales.eligibility.rules.EligibilityRules.PlanBenefit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads what an Eligibility check needs and hands it to {@link EligibilityRules}, which makes every decision. This
 * class has no decision logic of its own.
 */
@Service
public class EligibilityService {

	private final MemberRepository members;

	private final CoverageRepository coverages;

	private final BenefitRepository benefits;

	EligibilityService(MemberRepository members, CoverageRepository coverages, BenefitRepository benefits) {
		this.members = members;
		this.coverages = coverages;
		this.benefits = benefits;
	}

	/**
	 * @return the answer for this Member, procedure, and Service date, judged as of the Service date
	 * @throws MemberNotFoundException if no Member has this ID, which is different from coverage not being active
	 */
	@Transactional(readOnly = true)
	public EligibilityResult check(String memberId, String procedureCode, LocalDate serviceDate) {
		if (!members.existsById(memberId)) {
			throw new MemberNotFoundException(memberId);
		}
		var coverage = coverages.findActiveOn(memberId, serviceDate).orElse(null);
		if (coverage == null) {
			// With no Coverage there is no Plan whose Benefit could be loaded.
			return EligibilityRules.check(serviceDate, null, null);
		}
		var benefit = benefits.findByPlanIdAndProcedureCode(coverage.getPlan().getId(), procedureCode)
			.map(EligibilityService::planBenefit)
			.orElse(null);
		return EligibilityRules.check(serviceDate, activeCoverage(coverage), benefit);
	}

	private static ActiveCoverage activeCoverage(Coverage coverage) {
		var plan = coverage.getPlan();
		return new ActiveCoverage(plan.getId(), plan.getName(), plan.getPlanType());
	}

	private static PlanBenefit planBenefit(Benefit benefit) {
		return new PlanBenefit(benefit.isCovered(), benefit.getPriorAuthRequired(), benefit.getDeductibleApplies(),
				benefit.getCopay(), benefit.getCoinsurancePercent());
	}

}
