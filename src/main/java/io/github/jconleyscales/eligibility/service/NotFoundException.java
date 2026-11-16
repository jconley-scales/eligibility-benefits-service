package io.github.jconleyscales.eligibility.service;

/**
 * No record has the requested ID. This is a data error, distinct from a record that exists but has nothing to
 * show, such as a Member whose coverage isn't active or a Plan with no Benefits.
 */
public abstract class NotFoundException extends RuntimeException {

	protected NotFoundException(String recordType, String id) {
		super("No " + recordType + " with ID " + id + ".");
	}

}
