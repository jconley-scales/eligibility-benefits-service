-- SYNTHETIC DATA ONLY. Every Plan, Member, and Coverage below is invented for
-- testing and demonstration; none describes a real person or insurance product.
-- Each Member stands for one edge case; the README maps Member IDs to edge cases.
-- Benefits and Accumulators attach to these Plans and Coverages in later migrations.

INSERT INTO plan (id, name, plan_type, deductible, out_of_pocket_maximum) VALUES
    ('PPO-LOW-DED',  'Synthetic Low-Deductible PPO',  'PPO',  500.00, 3000.00),
    ('HMO-STANDARD', 'Synthetic Standard HMO',        'HMO',  250.00, 4000.00),
    ('PPO-HIGH-DED', 'Synthetic High-Deductible PPO', 'PPO', 3000.00, 7000.00);

INSERT INTO member (id, first_name, last_name, date_of_birth, synthetic) VALUES
    ('SYN-0001', 'Avery',   'Lindqvist', '1984-03-12', true),
    ('SYN-0002', 'Marcus',  'Bell',      '1971-11-05', true),
    ('SYN-0003', 'Priya',   'Raman',     '1966-06-18', true),
    ('SYN-0004', 'Rosa',    'Delgado',   '1958-01-22', true),
    ('SYN-0005', 'Theo',    'Nakamura',  '1999-09-09', true),
    ('SYN-0006', 'Hannah',  'Brooks',    '1990-02-27', true),
    ('SYN-0007', 'Jordan',  'Okafor',    '1979-08-30', true),
    ('SYN-0008', 'Elena',   'Petrova',   '1987-04-14', true),
    ('SYN-0009', 'Samuel',  'Achebe',    '1975-12-01', true),
    ('SYN-0010', 'Grace',   'Whitfield', '2001-07-23', true);

INSERT INTO coverage (member_id, plan_id, start_date, end_date) VALUES
    -- SYN-0001: active coverage with no end date (part of the deductible met)
    ('SYN-0001', 'PPO-LOW-DED',  '2025-01-01', NULL),
    -- SYN-0002: deductible fully met
    ('SYN-0002', 'PPO-HIGH-DED', '2026-01-01', NULL),
    -- SYN-0003: out-of-pocket maximum met
    ('SYN-0003', 'HMO-STANDARD', '2024-01-01', NULL),
    -- SYN-0004: coverage lapsed, ended in a previous year
    ('SYN-0004', 'PPO-LOW-DED',  '2023-01-01', '2025-12-31'),
    -- SYN-0005: coverage starting 2027-01-01
    ('SYN-0005', 'HMO-STANDARD', '2027-01-01', NULL),
    -- SYN-0006: coverage ending 2026-12-31
    ('SYN-0006', 'PPO-LOW-DED',  '2026-01-01', '2026-12-31'),
    -- SYN-0007: plan switch, one Coverage ending June 30 and the next starting July 1.
    -- The later Coverage is inserted first so the API's ordering by start date is exercised.
    ('SYN-0007', 'HMO-STANDARD', '2026-07-01', NULL),
    ('SYN-0007', 'PPO-HIGH-DED', '2026-01-01', '2026-06-30'),
    -- SYN-0008: no Accumulator for the service year (it began in 2025, so totals
    -- for a different year can be recorded)
    ('SYN-0008', 'PPO-LOW-DED',  '2025-01-01', NULL),
    -- SYN-0009: a Plan that excludes the requested procedure
    ('SYN-0009', 'HMO-STANDARD', '2026-01-01', NULL),
    -- SYN-0010: a Plan with no Benefit for the requested procedure
    ('SYN-0010', 'PPO-HIGH-DED', '2026-01-01', NULL);
