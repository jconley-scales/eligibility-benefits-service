package io.github.jconleyscales.eligibility.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

/**
 * Read-only: it extends the bare {@link Repository}, so there is no way to save or delete a Benefit.
 */
public interface BenefitRepository extends Repository<Benefit, Long> {

	/**
	 * A Plan's Benefits, one per procedure code, ordered by procedure code.
	 */
	List<Benefit> findByPlanIdOrderByProcedureCodeAsc(String planId);

	/**
	 * The Plan's Benefit for one procedure, or none when the Plan has no benefit on file for it.
	 */
	Optional<Benefit> findByPlanIdAndProcedureCode(String planId, String procedureCode);

}
