package io.github.jconleyscales.eligibility.web;

import java.time.LocalDate;
import java.util.List;

import io.github.jconleyscales.eligibility.persistence.Coverage;
import io.github.jconleyscales.eligibility.persistence.PlanType;
import org.jspecify.annotations.Nullable;

/**
 * A Member's coverage history: their Coverages, ordered by start date.
 */
record MemberCoverageResponse(String memberId, List<CoverageSummary> coverages) {

	static MemberCoverageResponse of(String memberId, List<Coverage> coverages) {
		return new MemberCoverageResponse(memberId, coverages.stream().map(CoverageSummary::of).toList());
	}

	/**
	 * One Coverage and its Plan. {@code endDate} is the last covered day, or null when ongoing.
	 */
	record CoverageSummary(String planId, String planName, PlanType planType, LocalDate startDate,
			@Nullable LocalDate endDate) {

		static CoverageSummary of(Coverage coverage) {
			var plan = coverage.getPlan();
			return new CoverageSummary(plan.getId(), plan.getName(), plan.getPlanType(), coverage.getStartDate(),
					coverage.getEndDate());
		}

	}

}
