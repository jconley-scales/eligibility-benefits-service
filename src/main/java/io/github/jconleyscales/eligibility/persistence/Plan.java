package io.github.jconleyscales.eligibility.persistence;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import org.hibernate.annotations.Immutable;

/**
 * A health insurance product with a type, a deductible, and an out-of-pocket maximum, in dollars.
 */
@Entity
@Immutable
public class Plan {

	@Id
	private String id;

	private String name;

	@Enumerated(EnumType.STRING)
	private PlanType planType;

	private BigDecimal deductible;

	private BigDecimal outOfPocketMaximum;

	protected Plan() {
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public PlanType getPlanType() {
		return planType;
	}

	public BigDecimal getDeductible() {
		return deductible;
	}

	public BigDecimal getOutOfPocketMaximum() {
		return outOfPocketMaximum;
	}

}
