package io.github.jconleyscales.eligibility.persistence;

/**
 * A label only: networks and referrals aren't modeled, so HMO and PPO plans follow the same rules.
 */
public enum PlanType {
	HMO,
	PPO
}
