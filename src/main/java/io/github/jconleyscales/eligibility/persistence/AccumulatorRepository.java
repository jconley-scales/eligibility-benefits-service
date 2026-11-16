package io.github.jconleyscales.eligibility.persistence;

import java.util.Optional;

import org.springframework.data.repository.Repository;

/**
 * Read-only: it extends the bare {@link Repository}, so there is no way to save or delete an Accumulator.
 */
public interface AccumulatorRepository extends Repository<Accumulator, Accumulator.Key> {

	/**
	 * The Coverage's totals for one plan year, or none when nothing has been recorded for that year.
	 */
	Optional<Accumulator> findByCoverageIdAndPlanYear(Long coverageId, int planYear);

}
