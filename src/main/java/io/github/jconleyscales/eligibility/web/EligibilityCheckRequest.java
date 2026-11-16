package io.github.jconleyscales.eligibility.web;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One Member, one procedure, and one Service date.
 */
@Schema(description = "One Member, one procedure, and one Service date.")
record EligibilityCheckRequest(@Schema(description = "The Member's ID.", example = "SYN-0001") String memberId,
		@Schema(description = "A CPT or HCPCS code.", example = "29881") String procedureCode,
		@Schema(description = """
				The date the procedure is or was performed, as an ISO date. The answer is judged as of this date, past \
				or future.""", example = "2026-03-15") LocalDate serviceDate) {
}
