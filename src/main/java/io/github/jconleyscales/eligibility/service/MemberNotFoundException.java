package io.github.jconleyscales.eligibility.service;

/**
 * No Member has the requested ID. This is a data error, distinct from a Member whose coverage isn't active.
 */
public class MemberNotFoundException extends RuntimeException {

	public MemberNotFoundException(String memberId) {
		super("No member with ID " + memberId + ".");
	}

}
