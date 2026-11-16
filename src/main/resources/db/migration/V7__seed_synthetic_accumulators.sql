-- SYNTHETIC DATA ONLY. Every Accumulator below is invented for testing and
-- demonstration; none describes a real person's spending.
-- Each row attaches to a seed Coverage, found by Member and start date. A lookup
-- that finds no Coverage leaves coverage_id null and fails this migration.
-- The README lists the Accumulator edge cases by Member.

INSERT INTO accumulator (coverage_id, plan_year, deductible_met, out_of_pocket_met) VALUES
    -- SYN-0001: part of the deductible met (PPO-LOW-DED: $500.00 / $3,000.00)
    ((SELECT id FROM coverage WHERE member_id = 'SYN-0001' AND start_date = '2025-01-01'),
        2026,  187.50,  412.75),
    -- SYN-0002: deductible fully met (PPO-HIGH-DED: $3,000.00 / $7,000.00)
    ((SELECT id FROM coverage WHERE member_id = 'SYN-0002' AND start_date = '2026-01-01'),
        2026, 3000.00, 3600.00),
    -- SYN-0003: out-of-pocket maximum met (HMO-STANDARD: $250.00 / $4,000.00)
    ((SELECT id FROM coverage WHERE member_id = 'SYN-0003' AND start_date = '2024-01-01'),
        2026,  250.00, 4000.00),
    -- SYN-0007: plan switch. Totals are recorded only on the first Coverage
    -- (PPO-HIGH-DED: $3,000.00 / $7,000.00, ending 2026-06-30). The HMO-STANDARD
    -- Coverage from 2026-07-01 has none, so it starts at zero.
    ((SELECT id FROM coverage WHERE member_id = 'SYN-0007' AND start_date = '2026-01-01'),
        2026, 1200.00, 1500.00),
    -- SYN-0008: no Accumulator for the plan year. The ongoing Coverage has
    -- totals for 2025 only (PPO-LOW-DED: $500.00 / $3,000.00), so a 2026 Service
    -- date finds none and another year's totals are shown to be ignored.
    ((SELECT id FROM coverage WHERE member_id = 'SYN-0008' AND start_date = '2025-01-01'),
        2025,  500.00, 1250.00);
