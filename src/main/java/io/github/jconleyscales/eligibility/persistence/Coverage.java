package io.github.jconleyscales.eligibility.persistence;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.Immutable;
import org.jspecify.annotations.Nullable;

/**
 * A Member's enrollment in one Plan for a date range. The end date is the last covered day, and no end
 * date means ongoing. The database guarantees a Member's Coverages never overlap.
 */
@Entity
@Immutable
public class Coverage {

	@Id
	private Long id;

	private String memberId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "plan_id")
	private Plan plan;

	private LocalDate startDate;

	private @Nullable LocalDate endDate;

	protected Coverage() {
	}

	public Long getId() {
		return id;
	}

	public Plan getPlan() {
		return plan;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	/**
	 * @return the last covered day, or {@code null} when the Coverage is ongoing
	 */
	public @Nullable LocalDate getEndDate() {
		return endDate;
	}

}
