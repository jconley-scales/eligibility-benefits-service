package io.github.jconleyscales.eligibility.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Immutable;

/**
 * A person enrolled with the insurer. Every Member is synthetic; the database refuses any that isn't. Only the
 * ID is mapped because nothing the API returns needs the rest of the row.
 */
@Entity
@Immutable
public class Member {

	@Id
	private String id;

	protected Member() {
	}

	public String getId() {
		return id;
	}

}
