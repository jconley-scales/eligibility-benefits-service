package io.github.jconleyscales.eligibility.persistence;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Immutable;
import org.jspecify.annotations.Nullable;

/**
 * A Plan's rule for one procedure: whether the Plan pays for it and, when it does, on what terms. The terms
 * (prior authorization, deductible, copay, coinsurance) are null exactly when the Plan doesn't pay for the
 * procedure; the database enforces that. A Plan has at most one Benefit per procedure code.
 */
@Entity
@Immutable
public class Benefit {

	@Id
	private Long id;

	private String planId;

	private String procedureCode;

	private boolean covered;

	private @Nullable Boolean priorAuthRequired;

	private @Nullable Boolean deductibleApplies;

	private @Nullable BigDecimal copay;

	private @Nullable Integer coinsurancePercent;

	protected Benefit() {
	}

	public String getProcedureCode() {
		return procedureCode;
	}

	/**
	 * @return whether the Plan pays for the procedure; when it doesn't, every term is null
	 */
	public boolean isCovered() {
		return covered;
	}

	public @Nullable Boolean getPriorAuthRequired() {
		return priorAuthRequired;
	}

	public @Nullable Boolean getDeductibleApplies() {
		return deductibleApplies;
	}

	/**
	 * @return the copay in dollars, with two decimals
	 */
	public @Nullable BigDecimal getCopay() {
		return copay;
	}

	/**
	 * @return the coinsurance as a whole-number percentage from 0 to 100
	 */
	public @Nullable Integer getCoinsurancePercent() {
		return coinsurancePercent;
	}

}
