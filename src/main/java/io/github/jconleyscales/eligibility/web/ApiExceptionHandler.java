package io.github.jconleyscales.eligibility.web;

import static java.util.stream.Collectors.joining;

import java.time.LocalDate;

import io.github.jconleyscales.eligibility.service.NotFoundException;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Renders the API's errors as RFC 9457 Problem Details. The base class covers Spring MVC's own errors, such as an
 * unknown path; this adds the service's, such as an unknown Member or Plan, and replaces the base class's generic
 * detail for a bad request with one naming what the client must fix.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler
	ProblemDetail notFound(NotFoundException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	/**
	 * A body that parsed but broke a constraint: one sentence per invalid field, such as "memberId is required.",
	 * sorted so the detail is the same on every run.
	 */
	@Override
	protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		var detail = exception.getFieldErrors()
			.stream()
			.map(error -> error.getField() + " " + error.getDefaultMessage() + ".")
			.sorted()
			.collect(joining(" "));
		return badRequest(exception, detail, headers, status, request);
	}

	/**
	 * A body that couldn't be read into the request: not JSON, not an object, or a field of the wrong type, such as
	 * a Service date that isn't an ISO date.
	 */
	@Override
	protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return badRequest(exception, unreadableBodyDetail(exception.getCause()), headers, status, request);
	}

	private @Nullable ResponseEntity<Object> badRequest(Exception exception, String detail, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		return handleExceptionInternal(exception, ProblemDetail.forStatusAndDetail(status, detail), headers, status,
				request);
	}

	/**
	 * @param cause Jackson's reason the body couldn't be read, or {@code null} when there is no body
	 */
	private static String unreadableBodyDetail(@Nullable Throwable cause) {
		if (cause instanceof MismatchedInputException mismatch && fieldName(mismatch) instanceof String field) {
			return field + " must be " + expectedValue(mismatch.getTargetType()) + ".";
		}
		if (cause instanceof StreamReadException) {
			return "The request body is not valid JSON.";
		}
		return "The request body must be a JSON object.";
	}

	/**
	 * The request field whose value didn't fit, or {@code null} when the problem is the body as a whole, such as
	 * an array where an object belongs.
	 */
	private static @Nullable String fieldName(MismatchedInputException mismatch) {
		var path = mismatch.getPath();
		return path.isEmpty() ? null : path.getLast().getPropertyName();
	}

	private static String expectedValue(@Nullable Class<?> targetType) {
		if (targetType == LocalDate.class) {
			return "an ISO date (YYYY-MM-DD)";
		}
		if (targetType == String.class) {
			return "a string";
		}
		return "a valid value";
	}

}
