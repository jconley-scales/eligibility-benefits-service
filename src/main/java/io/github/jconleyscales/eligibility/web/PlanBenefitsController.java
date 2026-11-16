package io.github.jconleyscales.eligibility.web;

import io.github.jconleyscales.eligibility.service.PlanBenefitsService;
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
@RequestMapping("/v1/plans")
@Tag(name = "Plans")
class PlanBenefitsController {

	private final PlanBenefitsService planBenefits;

	PlanBenefitsController(PlanBenefitsService planBenefits) {
		this.planBenefits = planBenefits;
	}

	@GetMapping("/{planId}/benefits")
	@Operation(operationId = "getPlanBenefits", summary = "List a Plan's Benefits",
			description = "The Plan's details and its Benefits, ordered by procedure code: which procedures the Plan "
					+ "pays for, and on what terms.")
	@ApiResponse(responseCode = "200", description = "The Plan and its Benefits.")
	@ApiResponse(responseCode = "404", description = "No Plan has this ID.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailsSchema.class)))
	PlanBenefitsResponse benefits(
			@Parameter(description = "The Plan's ID.", example = "HMO-STANDARD") @PathVariable String planId) {
		return PlanBenefitsResponse.of(planBenefits.planBenefits(planId));
	}

}
