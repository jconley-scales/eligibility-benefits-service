package io.github.jconleyscales.eligibility.web;

import io.github.jconleyscales.eligibility.service.EligibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/eligibility")
@Tag(name = "Eligibility")
class EligibilityController {

	private final EligibilityService eligibility;

	EligibilityController(EligibilityService eligibility) {
		this.eligibility = eligibility;
	}

	@PostMapping("/check")
	@Operation(operationId = "checkEligibility", summary = "Check eligibility",
			description = """
					One answer for one Member, one procedure, and one Service date, judged as of the Service date \
					rather than today: whether coverage is active, which Plan and plan year the answer comes from, \
					and the Plan's Benefit for the procedure.""")
	@ApiResponse(responseCode = "200", description = """
			The answer. Coverage that isn't active, and a procedure with no benefit on file, are answers too, not \
			errors.""")
	@ApiResponse(responseCode = "404", description = "No Member has this ID.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailsSchema.class)))
	EligibilityCheckResponse check(@RequestBody EligibilityCheckRequest request) {
		return EligibilityCheckResponse.of(request,
				eligibility.check(request.memberId(), request.procedureCode(), request.serviceDate()));
	}

}
