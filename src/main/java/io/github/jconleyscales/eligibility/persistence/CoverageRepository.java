package io.github.jconleyscales.eligibility.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
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

	/**
	 * The Member's Coverage active on the Service date, with its Plan loaded in the same query. The end date is a
	 * covered day and no end date means ongoing. The database allows at most one Coverage per Member on any date,
	 * so this finds one or none.
	 */
	@Query("""
			select c from Coverage c join fetch c.plan
			where c.memberId = :memberId
			  and c.startDate <= :serviceDate
			  and (c.endDate is null or c.endDate >= :serviceDate)
			""")
	Optional<Coverage> findActiveOn(String memberId, LocalDate serviceDate);

}
