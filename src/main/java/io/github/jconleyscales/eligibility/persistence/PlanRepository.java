package io.github.jconleyscales.eligibility.persistence;

import java.util.Optional;

import org.springframework.data.repository.Repository;

/**
 * Read-only: it extends the bare {@link Repository}, so there is no way to save or delete a Plan.
 */
public interface PlanRepository extends Repository<Plan, String> {

	Optional<Plan> findById(String id);

}
