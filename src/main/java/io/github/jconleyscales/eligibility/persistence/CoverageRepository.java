package io.github.jconleyscales.eligibility.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

/**
 * Read-only: it extends the bare {@link Repository}, so there is no way to save or delete a Coverage.
 */
public interface CoverageRepository extends Repository<Coverage, Long> {

	/**
	 * A Member's Coverages, earliest first, each with its Plan loaded in the same query.
	 */
	@EntityGraph(attributePaths = "plan")
	List<Coverage> findByMemberIdOrderByStartDateAsc(String memberId);

}
