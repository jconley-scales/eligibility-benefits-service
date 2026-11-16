package io.github.jconleyscales.eligibility.web;

import io.github.jconleyscales.eligibility.service.MemberCoverageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/members")
@Tag(name = "Members")
class MemberCoverageController {

	private final MemberCoverageService memberCoverage;

	MemberCoverageController(MemberCoverageService memberCoverage) {
		this.memberCoverage = memberCoverage;
	}

	@GetMapping("/{memberId}/coverage")
	@Operation(operationId = "getMemberCoverage", summary = "List a Member's coverage history",
			description = "The Member's Coverages, each with its Plan, ordered by start date.")
	@ApiResponse(responseCode = "200", description = "The Member's coverage history.")
	@ApiResponse(responseCode = "404", description = "No Member has this ID.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailsSchema.class)))
	MemberCoverageResponse coverage(
			@Parameter(description = "The Member's ID.", example = "SYN-0007") @PathVariable String memberId) {
		return MemberCoverageResponse.of(memberId, memberCoverage.coverageHistory(memberId));
	}

}
