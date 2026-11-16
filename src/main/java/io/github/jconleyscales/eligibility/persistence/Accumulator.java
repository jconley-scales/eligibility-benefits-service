package io.github.jconleyscales.eligibility.persistence;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import org.hibernate.annotations.Immutable;

/**
 * A Member's running totals of deductible and out-of-pocket spending under one Coverage in one plan year, in
 * dollars. Totals belong to one Coverage, so a plan switch starts new totals at zero. A Coverage has at most one
 * Accumulator per plan year.
 */
@Entity
@Immutable
@IdClass(Accumulator.Key.class)
public class Accumulator {

	@Id
	private Long coverageId;

	@Id
	private Integer planYear;

	private BigDecimal deductibleMet;

	private BigDecimal outOfPocketMet;

	protected Accumulator() {
	}

	/**
	 * @return the deductible met so far in the plan year, 0 or more; it can exceed the Plan's deductible
	 */
	public BigDecimal getDeductibleMet() {
		return deductibleMet;
	}

	/**
	 * @return the out-of-pocket spending so far in the plan year, 0 or more; it can exceed the Plan's out-of-pocket
	 * maximum
	 */
	public BigDecimal getOutOfPocketMet() {
		return outOfPocketMet;
	}

	/**
	 * The primary key: one Coverage and one plan year.
	 */
	public record Key(Long coverageId, Integer planYear) {
	}

}
