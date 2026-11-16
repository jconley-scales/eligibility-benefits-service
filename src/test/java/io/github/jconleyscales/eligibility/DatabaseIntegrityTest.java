package io.github.jconleyscales.eligibility;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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
		insertPlan(PLAN_ID);
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

	@Test
	void aSecondBenefitForTheSamePlanAndProcedureIsRejected() {
		insertCoveredBenefit(PLAN_ID, "72148");

		assertThatThrownBy(() -> insertBenefit(PLAN_ID, "72148", false, null, null, null, null))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("benefit_one_per_plan_and_procedure");
	}

	@Test
	void differentPlansMayEachHaveABenefitForTheSameProcedure() {
		insertPlan("TEST-OTHER-PLAN");
		insertCoveredBenefit(PLAN_ID, "72148");

		assertThatCode(() -> insertCoveredBenefit("TEST-OTHER-PLAN", "72148")).doesNotThrowAnyException();
	}

	@Test
	void anExcludedProcedureWithNoTermsIsAccepted() {
		assertThatCode(() -> insertBenefit(PLAN_ID, "95810", false, null, null, null, null))
			.doesNotThrowAnyException();
	}

	/** The terms say how the Plan pays, so an excluded procedure has none and a covered one has all four. */
	@ParameterizedTest(name = "covered={0}, priorAuthRequired={1}, deductibleApplies={2}, copay={3}, coinsurance={4}")
	@CsvSource(nullValues = "null", textBlock = """
			false, false, false, 0.00, 0
			false, null,  null,  0.00, null
			true,  null,  null,  null, null
			true,  true,  true,  null, 20
			""")
	void termsThatDoNotMatchWhetherTheProcedureIsCoveredAreRejected(boolean covered,
			@Nullable Boolean priorAuthRequired, @Nullable Boolean deductibleApplies, @Nullable BigDecimal copay,
			@Nullable Integer coinsurancePercent) {
		assertThatThrownBy(() -> insertBenefit(PLAN_ID, "72148", covered, priorAuthRequired, deductibleApplies,
				copay, coinsurancePercent))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("benefit_terms_only_when_covered");
	}

	@Test
	void aNegativeCopayIsRejected() {
		assertThatThrownBy(() -> insertBenefit(PLAN_ID, "97110", true, false, false, new BigDecimal("-0.01"), 0))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("benefit_copay_not_negative");
	}

	@ParameterizedTest(name = "coinsurance {0}%")
	@ValueSource(ints = { -1, 101 })
	void coinsuranceOutsideZeroToOneHundredPercentIsRejected(int coinsurancePercent) {
		assertThatThrownBy(() -> insertBenefit(PLAN_ID, "97110", true, false, false, BigDecimal.ZERO,
				coinsurancePercent))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("benefit_coinsurance_percent_0_to_100");
	}

	/** Totals belong to one Coverage and one plan year, so a lookup by both finds one Accumulator or none. */
	@Test
	void aSecondAccumulatorForTheSameCoverageAndPlanYearIsRejected() {
		var coverageId = insertCoverage(LocalDate.of(2025, 1, 1), null);
		insertAccumulator(coverageId, 2026, new BigDecimal("100.00"), new BigDecimal("200.00"));

		assertThatThrownBy(() -> insertAccumulator(coverageId, 2026, BigDecimal.ZERO, BigDecimal.ZERO))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("accumulator_one_per_coverage_and_plan_year");
	}

	@Test
	void aCoverageMayHaveAnAccumulatorForEachPlanYear() {
		var coverageId = insertCoverage(LocalDate.of(2025, 1, 1), null);
		insertAccumulator(coverageId, 2025, new BigDecimal("100.00"), new BigDecimal("200.00"));

		assertThatCode(() -> insertAccumulator(coverageId, 2026, BigDecimal.ZERO, BigDecimal.ZERO))
			.doesNotThrowAnyException();
	}

	@ParameterizedTest(name = "deductible met {0}, out-of-pocket met {1}")
	@CsvSource(textBlock = """
			-0.01, 0.00,  accumulator_deductible_met_not_negative
			0.00,  -0.01, accumulator_out_of_pocket_met_not_negative
			""")
	void negativeAccumulatorTotalsAreRejected(BigDecimal deductibleMet, BigDecimal outOfPocketMet,
			String constraint) {
		var coverageId = insertCoverage(LocalDate.of(2026, 1, 1), null);

		assertThatThrownBy(() -> insertAccumulator(coverageId, 2026, deductibleMet, outOfPocketMet))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining(constraint);
	}

	private void insertMember(String id, boolean synthetic) {
		jdbc.update("""
				INSERT INTO member (id, first_name, last_name, date_of_birth, synthetic)
				VALUES (?, 'Test', 'Member', ?, ?)
				""", id, LocalDate.of(1990, 1, 1), synthetic);
	}

	private void insertPlan(String id) {
		jdbc.update("""
				INSERT INTO plan (id, name, plan_type, deductible, out_of_pocket_maximum)
				VALUES (?, 'Synthetic Test Plan', 'PPO', 100.00, 1000.00)
				""", id);
	}

	private Long insertCoverage(LocalDate startDate, @Nullable LocalDate endDate) {
		return insertCoverage(MEMBER_ID, startDate, endDate);
	}

	/** @return the new Coverage's generated ID */
	private Long insertCoverage(String memberId, LocalDate startDate, @Nullable LocalDate endDate) {
		return jdbc.queryForObject("""
				INSERT INTO coverage (member_id, plan_id, start_date, end_date) VALUES (?, ?, ?, ?)
				RETURNING id
				""", Long.class, memberId, PLAN_ID, startDate, endDate);
	}

	private void insertAccumulator(Long coverageId, int planYear, BigDecimal deductibleMet,
			BigDecimal outOfPocketMet) {
		jdbc.update("""
				INSERT INTO accumulator (coverage_id, plan_year, deductible_met, out_of_pocket_met)
				VALUES (?, ?, ?, ?)
				""", coverageId, planYear, deductibleMet, outOfPocketMet);
	}

	private void insertCoveredBenefit(String planId, String procedureCode) {
		insertBenefit(planId, procedureCode, true, true, true, new BigDecimal("25.00"), 20);
	}

	private void insertBenefit(String planId, String procedureCode, boolean covered,
			@Nullable Boolean priorAuthRequired, @Nullable Boolean deductibleApplies, @Nullable BigDecimal copay,
			@Nullable Integer coinsurancePercent) {
		jdbc.update("""
				INSERT INTO benefit (plan_id, procedure_code, covered, prior_auth_required, deductible_applies,
				                     copay, coinsurance_percent)
				VALUES (?, ?, ?, ?, ?, ?, ?)
				""", planId, procedureCode, covered, priorAuthRequired, deductibleApplies, copay, coinsurancePercent);
	}

}
