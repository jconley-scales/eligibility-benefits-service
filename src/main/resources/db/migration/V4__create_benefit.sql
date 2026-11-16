-- A Benefit is a Plan's rule for one procedure: whether the Plan pays for it and,
-- when it does, on what terms. Money is dollars with two decimals; coinsurance is
-- a whole-number percentage. Named benefit, not benefit_rule, to match the glossary.
CREATE TABLE benefit (
    id                  bigint         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    plan_id             text           NOT NULL REFERENCES plan (id),
    procedure_code      text           NOT NULL,
    covered             boolean        NOT NULL,
    prior_auth_required boolean,
    deductible_applies  boolean,
    copay               numeric(10, 2)
        CONSTRAINT benefit_copay_not_negative CHECK (copay >= 0),
    coinsurance_percent integer
        CONSTRAINT benefit_coinsurance_percent_0_to_100
        CHECK (coinsurance_percent BETWEEN 0 AND 100),
    -- Each Plan has at most one Benefit per procedure code, so looking up a Plan's
    -- Benefit for a procedure finds one row or none.
    CONSTRAINT benefit_one_per_plan_and_procedure UNIQUE (plan_id, procedure_code),
    -- The terms (prior authorization, deductible, copay, coinsurance) say how the
    -- Plan pays, so a Benefit the Plan pays for has all four and an excluded
    -- procedure has none. An excluded procedure with a $0.00 copay would read as free.
    CONSTRAINT benefit_terms_only_when_covered CHECK (
        num_nonnulls(prior_auth_required, deductible_applies, copay, coinsurance_percent)
            = CASE WHEN covered THEN 4 ELSE 0 END
    )
);
