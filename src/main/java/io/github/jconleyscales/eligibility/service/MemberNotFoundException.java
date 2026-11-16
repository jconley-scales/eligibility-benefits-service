package io.github.jconleyscales.eligibility.service;

/**
 * No Member has the requested ID. This is a data error, distinct from a Member whose coverage isn't active.
 */
public class MemberNotFoundException extends NotFoundException {

	public MemberNotFoundException(String memberId) {
		super("member", memberId);
	}

}
