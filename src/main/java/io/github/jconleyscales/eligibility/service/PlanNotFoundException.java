package io.github.jconleyscales.eligibility.service;

/**
 * No Plan has the requested ID. This is different from a Plan that has no Benefits.
 */
public class PlanNotFoundException extends NotFoundException {

	public PlanNotFoundException(String planId) {
		super("plan", planId);
	}

}
