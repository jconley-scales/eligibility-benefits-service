package io.github.jconleyscales.eligibility.web;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * One Member, one procedure, and one Service date. Each constraint's message follows the field name in the 400
 * response's detail, as in "memberId is required."
 */
@Schema(description = "One Member, one procedure, and one Service date.")
record EligibilityCheckRequest(
		@Schema(description = "The Member's ID.", example = "SYN-0001") @NotBlank(message = "is required") String memberId,
		@Schema(description = "The procedure's code, which must be " + PROCEDURE_CODE_FORMATS + ". Only the format is "
				+ "checked, not whether the code is in the CPT or HCPCS code sets.", example = "29881")
		@NotNull(message = "is required")
		@Pattern(regexp = PROCEDURE_CODE_PATTERN, message = "must be " + PROCEDURE_CODE_FORMATS) String procedureCode,
		@Schema(description = """
				The date the procedure is or was performed, as an ISO date. The answer is judged as of this date, past \
				or future.""", example = "2026-03-15") @NotNull(message = "is required")
		@JsonFormat(lenient = OptBoolean.FALSE) LocalDate serviceDate) {

	/**
	 * CPT, CPT Category II or III, or HCPCS Level II. A malformed code is rejected rather than answered as no
	 * benefit on file (ADR 0003). Anchored, because the contract publishes it as a JSON Schema {@code pattern},
	 * which is otherwise unanchored.
	 */
	static final String PROCEDURE_CODE_PATTERN = "^([0-9]{5}|[0-9]{4}[FTU]|[A-Z][0-9]{4})$";

	/** {@link #PROCEDURE_CODE_PATTERN} in words, for the contract and for the 400's detail. */
	static final String PROCEDURE_CODE_FORMATS = "a CPT code (five digits), a CPT Category II or III code (four "
			+ "digits then F, T, or U), or a HCPCS Level II code (an upper-case letter then four digits)";

}
