-- SYNTHETIC DATA ONLY. Every Benefit below is invented for testing and
-- demonstration; none describes a real insurance product.
-- Each seed Plan has Benefits for the five pilot procedures the Prior Authorization
-- Copilot uses, with two gaps on purpose; the README lists them by Member:
--   * HMO-STANDARD excludes 95810 (not covered): Member SYN-0009.
--   * PPO-HIGH-DED has no Benefit for 97110 (no benefit on file): Member SYN-0010.
--
-- Pilot procedures:
--   29881  knee arthroscopy (CPT)
--   72148  lumbar spine MRI (CPT)
--   95810  sleep study (CPT)
--   97110  physical therapy (CPT)
--   E0601  CPAP device (HCPCS)

INSERT INTO benefit
    (plan_id, procedure_code, covered, prior_auth_required, deductible_applies, copay, coinsurance_percent)
VALUES
    ('PPO-LOW-DED',  '29881', true,  true,  true,    0.00, 20),
    ('PPO-LOW-DED',  '72148', true,  false, true,    0.00, 20),
    ('PPO-LOW-DED',  '95810', true,  true,  true,    0.00, 20),
    ('PPO-LOW-DED',  '97110', true,  false, false,  30.00,  0),
    ('PPO-LOW-DED',  'E0601', true,  true,  true,    0.00, 20),

    ('HMO-STANDARD', '29881', true,  true,  false, 150.00,  0),
    ('HMO-STANDARD', '72148', true,  true,  false,  75.00,  0),
    -- Excluded, so it has no terms: the Plan doesn't pay for it at all.
    ('HMO-STANDARD', '95810', false, NULL,  NULL,    NULL, NULL),
    ('HMO-STANDARD', '97110', true,  false, false,  25.00,  0),
    ('HMO-STANDARD', 'E0601', true,  true,  true,    0.00, 10),

    ('PPO-HIGH-DED', '29881', true,  true,  true,    0.00, 30),
    ('PPO-HIGH-DED', '72148', true,  true,  true,    0.00, 30),
    ('PPO-HIGH-DED', '95810', true,  true,  true,    0.00, 30),
    -- No row for 97110: this Plan has no benefit on file for it.
    ('PPO-HIGH-DED', 'E0601', true,  true,  true,    0.00, 30);
