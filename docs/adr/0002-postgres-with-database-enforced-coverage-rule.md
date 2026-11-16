# Postgres, with the one-coverage-at-a-time rule enforced by the database

A Member has at most one Coverage on any date. Coordination of benefits is out of scope, and the Eligibility check relies on the rule: it looks up *the* Coverage active on a Service date. We keep the data in Postgres and the database enforces the rule. An exclusion constraint on `coverage` rejects two rows for the same Member whose date ranges overlap:

```sql
EXCLUDE USING gist (member_id WITH =, daterange(start_date, end_date, '[]') WITH &&)
```

The `'[]'` bounds make both ends covered days, because the end date is the last covered day. So a plan switch with one Coverage ending June 30 and the next starting July 1 is accepted, and two Coverages sharing even one day are rejected. A null end date gives an unbounded range, so an ongoing Coverage overlaps everything after its start. `btree_gist` lets the GiST index compare `member_id` with plain equality. A `CHECK (synthetic)` on `member` keeps real patient data out the same way.

## Considered options

- **Check in application code before inserting.** The API never writes, so the only writers are migrations, scripts, and whatever writes later. A check inside the service protects none of them. A check-then-insert also races under concurrent writes unless every writer locks.
- **A trigger.** It would work, but it is procedural code to read and test, where the constraint is one declarative line.
- **An in-memory database such as H2 for tests.** It has no exclusion constraints, so tests against it couldn't prove the rule.
- **MySQL.** It has no exclusion constraints either, so the rule would fall back to a trigger or application code.

## Consequences

- The schema is Postgres-specific: Flyway migration V1 enables `btree_gist`.
- Tests that touch the database run against a real Postgres 18 through Testcontainers, so running them needs Docker. Database tests insert rows directly to prove the rule, since the read-only API can't.
- Code that finds the Coverage active on a Service date can rely on at most one row matching.
