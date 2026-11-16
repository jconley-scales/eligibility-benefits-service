-- A Plan is a health insurance product. Money is dollars with two decimals.
CREATE TABLE plan (
    id                    text           PRIMARY KEY,
    name                  text           NOT NULL,
    plan_type             text           NOT NULL
        CONSTRAINT plan_type_is_hmo_or_ppo CHECK (plan_type IN ('HMO', 'PPO')),
    deductible            numeric(10, 2) NOT NULL
        CONSTRAINT plan_deductible_not_negative CHECK (deductible >= 0),
    out_of_pocket_maximum numeric(10, 2) NOT NULL,
    CONSTRAINT plan_out_of_pocket_maximum_at_least_deductible
        CHECK (out_of_pocket_maximum >= deductible)
);

-- A Member is one synthetic person. The database refuses any Member not marked
-- synthetic, so real patient data can't slip in.
CREATE TABLE member (
    id            text    PRIMARY KEY,
    first_name    text    NOT NULL,
    last_name     text    NOT NULL,
    date_of_birth date    NOT NULL,
    synthetic     boolean NOT NULL
        CONSTRAINT member_is_synthetic CHECK (synthetic)
);

-- A Coverage is a Member's enrollment in one Plan for a date range. The end date
-- is the last covered day; no end date means ongoing.
CREATE TABLE coverage (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id  text   NOT NULL REFERENCES member (id),
    plan_id    text   NOT NULL REFERENCES plan (id),
    start_date date   NOT NULL,
    end_date   date,
    CONSTRAINT coverage_ends_on_or_after_start
        CHECK (end_date IS NULL OR end_date >= start_date),
    -- A Member has at most one Coverage on any date. '[]' makes both ends covered
    -- days, so a Coverage ending June 30 and one starting July 1 are allowed, but
    -- two that share even one day are not. A NULL end makes the range unbounded.
    -- btree_gist (V1) lets the GiST index compare member_id with plain equality.
    CONSTRAINT coverage_no_overlap EXCLUDE USING gist (
        member_id WITH =,
        daterange(start_date, end_date, '[]') WITH &&
    )
);
