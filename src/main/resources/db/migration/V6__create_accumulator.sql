-- An Accumulator is a Member's running totals of deductible and out-of-pocket
-- spending under one Coverage in one plan year (the calendar year). Money is
-- dollars with two decimals.
CREATE TABLE accumulator (
    coverage_id       bigint         NOT NULL REFERENCES coverage (id),
    plan_year         integer        NOT NULL,
    deductible_met    numeric(10, 2) NOT NULL
        CONSTRAINT accumulator_deductible_met_not_negative CHECK (deductible_met >= 0),
    out_of_pocket_met numeric(10, 2) NOT NULL
        CONSTRAINT accumulator_out_of_pocket_met_not_negative CHECK (out_of_pocket_met >= 0),
    -- Totals belong to one Coverage, so a Member who switches Plans mid-year
    -- starts the new Coverage's totals at zero. Each Coverage has at most one
    -- Accumulator per plan year, so looking one up finds one row or none.
    CONSTRAINT accumulator_one_per_coverage_and_plan_year PRIMARY KEY (coverage_id, plan_year)
);
-- Totals can exceed the Plan's deductible or out-of-pocket maximum: a CHECK
-- can't see the plan table. The Eligibility rules never report less than zero
-- remaining.
