package io.github.jconleyscales.eligibility.service;

import java.util.List;

import io.github.jconleyscales.eligibility.persistence.Coverage;
import io.github.jconleyscales.eligibility.persistence.CoverageRepository;
import io.github.jconleyscales.eligibility.persistence.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Looks up a Member's coverage history.
 */
@Service
public class MemberCoverageService {

	private final MemberRepository members;

	private final CoverageRepository coverages;

	MemberCoverageService(MemberRepository members, CoverageRepository coverages) {
		this.members = members;
		this.coverages = coverages;
	}

	/**
	 * @return the Member's Coverages ordered by start date, each with its Plan loaded
	 * @throws MemberNotFoundException if no Member has this ID
	 */
	@Transactional(readOnly = true)
	public List<Coverage> coverageHistory(String memberId) {
		if (!members.existsById(memberId)) {
			throw new MemberNotFoundException(memberId);
		}
		return coverages.findByMemberIdOrderByStartDateAsc(memberId);
	}

}
