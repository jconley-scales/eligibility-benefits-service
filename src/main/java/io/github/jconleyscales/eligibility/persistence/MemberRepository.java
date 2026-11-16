package io.github.jconleyscales.eligibility.persistence;

import org.springframework.data.repository.Repository;

/**
 * Read-only: it extends the bare {@link Repository}, so there is no way to save or delete a Member.
 */
public interface MemberRepository extends Repository<Member, String> {

	boolean existsById(String id);

}
