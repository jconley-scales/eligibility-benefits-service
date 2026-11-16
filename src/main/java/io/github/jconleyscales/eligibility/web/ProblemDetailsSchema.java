package io.github.jconleyscales.eligibility.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * How the API contract documents an error body: RFC 9457 Problem Details, as {@link ApiExceptionHandler} writes
 * it. Only the contract uses this type; the service never creates one. Documenting Spring's
 * {@link org.springframework.http.ProblemDetail} directly would show a nested {@code properties} member that the
 * API never sends, because Spring writes extra properties at the top level.
 */
@Schema(name = "ProblemDetails", description = "An error, in the RFC 9457 Problem Details format.")
record ProblemDetailsSchema(
		@Schema(description = "Identifies the problem type. Left out when it is about:blank, meaning the HTTP status says it all.",
				format = "uri-reference") String type,
		@Schema(description = "A short summary of the problem type, such as \"Not Found\".") String title,
		@Schema(description = "The HTTP status code.") int status,
		@Schema(description = "What went wrong with this request.") String detail,
		@Schema(description = "The path of the request that failed.", format = "uri-reference") String instance) {
}
