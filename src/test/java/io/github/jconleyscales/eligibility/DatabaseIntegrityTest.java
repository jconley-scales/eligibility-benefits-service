package io.github.jconleyscales.eligibility;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * The database's own integrity rules. The API is read-only and can't prove them, so these tests insert
 * rows directly. Each test runs in a transaction that is rolled back, leaving the seed data untouched.
 */
@IntegrationTest
@Transactional
class DatabaseIntegrityTest {

	private static final String MEMBER_ID = "TEST-MEMBER";

	private static final String PLAN_ID = "TEST-PLAN";

	@Autowired
	JdbcTemplate jdbc;

	@BeforeEach
	void insertASyntheticMemberAndAPlan() {
		insertMember(MEMBER_ID, true);
		jdbc.update("""
				INSERT INTO plan (id, name, plan_type, deductible, out_of_pocket_maximum)
				VALUES (?, 'Synthetic Test Plan', 'PPO', 100.00, 1000.00)
				""", PLAN_ID);
	}

	@Test
	void overlappingCoveragesForOneMemberAreRejected() {
		insertCoverage(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

		assertThatThrownBy(() -> insertCoverage(LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31)))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("coverage_no_overlap");
	}

	@Test
	void coveragesSharingOnlyTheEndDateAreRejectedBecauseTheEndDateIsACoveredDay() {
		insertCoverage(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));

		assertThatThrownBy(() -> insertCoverage(LocalDate.of(2026, 6, 30), null))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("coverage_no_overlap");
	}

	@Test
	void anOngoingCoverageOverlapsEveryLaterCoverage() {
		insertCoverage(LocalDate.of(2025, 1, 1), null);

		assertThatThrownBy(() -> insertCoverage(LocalDate.of(2030, 1, 1), LocalDate.of(2030, 12, 31)))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("coverage_no_overlap");
	}

	@Test
	void backToBackCoveragesAreAccepted() {
		insertCoverage(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));

		assertThatCode(() -> insertCoverage(LocalDate.of(2026, 7, 1), null)).doesNotThrowAnyException();
	}

	@Test
	void differentMembersMayHaveCoverageOnTheSameDates() {
		insertMember("TEST-OTHER-MEMBER", true);
		insertCoverage(LocalDate.of(2026, 1, 1), null);

		assertThatCode(() -> insertCoverage("TEST-OTHER-MEMBER", LocalDate.of(2026, 1, 1), null))
			.doesNotThrowAnyException();
	}

	@Test
	void aCoverageEndingBeforeItStartsIsRejected() {
		assertThatThrownBy(() -> insertCoverage(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 6, 30)))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("coverage_ends_on_or_after_start");
	}

	@Test
	void aMemberNotMarkedSyntheticIsRejected() {
		assertThatThrownBy(() -> insertMember("TEST-REAL-PERSON", false))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("member_is_synthetic");
	}

	private void insertMember(String id, boolean synthetic) {
		jdbc.update("""
				INSERT INTO member (id, first_name, last_name, date_of_birth, synthetic)
				VALUES (?, 'Test', 'Member', ?, ?)
				""", id, LocalDate.of(1990, 1, 1), synthetic);
	}

	private void insertCoverage(LocalDate startDate, @Nullable LocalDate endDate) {
		insertCoverage(MEMBER_ID, startDate, endDate);
	}

	private void insertCoverage(String memberId, LocalDate startDate, @Nullable LocalDate endDate) {
		jdbc.update("INSERT INTO coverage (member_id, plan_id, start_date, end_date) VALUES (?, ?, ?, ?)",
				memberId, PLAN_ID, startDate, endDate);
	}

}
