package io.github.jconleyscales.eligibility.web;

import io.github.jconleyscales.eligibility.service.MemberCoverageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/members")
class MemberCoverageController {

	private final MemberCoverageService memberCoverage;

	MemberCoverageController(MemberCoverageService memberCoverage) {
		this.memberCoverage = memberCoverage;
	}

	@GetMapping("/{memberId}/coverage")
	MemberCoverageResponse coverage(@PathVariable String memberId) {
		return MemberCoverageResponse.of(memberId, memberCoverage.coverageHistory(memberId));
	}

}
