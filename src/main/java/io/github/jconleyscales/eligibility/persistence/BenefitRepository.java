package io.github.jconleyscales.eligibility.persistence;

import java.util.List;

import org.springframework.data.repository.Repository;

/**
 * Read-only: it extends the bare {@link Repository}, so there is no way to save or delete a Benefit.
 */
public interface BenefitRepository extends Repository<Benefit, Long> {

	/**
	 * A Plan's Benefits, one per procedure code, ordered by procedure code.
	 */
	List<Benefit> findByPlanIdOrderByProcedureCodeAsc(String planId);

}
