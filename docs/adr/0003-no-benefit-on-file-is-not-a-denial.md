# "No benefit on file" is not a denial

A Plan's Benefits say which procedures it pays for and on what terms. Sometimes the Plan has no Benefit at all for the requested procedure. The Prior Authorization Copilot sends anything other than active coverage with a `COVERED` Benefit to human review. If the service reported a missing Benefit as `NOT_COVERED`, the reviewer would read it as the Plan's own answer, and the request would in effect be denied because data was missing.

So the Eligibility check has three Benefit statuses, and the two negative ones are never confused:

- **`NOT_COVERED`** only when the Plan has a Benefit saying it doesn't pay for the procedure: a `benefit` row with `covered` false and no terms. This is the Plan's answer.
- **`NO_BENEFIT_ON_FILE`** when the Plan has no `benefit` row for the procedure, including any well-formed CPT or HCPCS code the Plan doesn't list. It means unknown, not unpaid, so a person decides.

`NO_BENEFIT_ON_FILE` comes back with 200, as an answer. A 404 is kept for a Member who doesn't exist, which is a data error. With no active coverage there is no Benefit at all, so neither status appears.

The status is only safe if the procedure code was well formed: a typo would otherwise come back as no benefit on file. Request validation rejects a malformed code with 400 for that reason. It checks only the format (CPT, CPT Category II or III, or HCPCS Level II), not the real code sets, so a well-formed code the Plan doesn't list is still no benefit on file.

## Considered options

- **Treat a missing Benefit as not covered.** The Plan's Benefits become a closed list, and anything unlisted is excluded. It is simple, and some real benefit setups work this way, but every gap in the data turns into a denial that looks like the Plan's decision. The seed data is written by hand, so gaps are likely.
- **Treat a missing Benefit as covered.** Gaps turn into approvals with no known cost share or prior authorization rule. That is worse for the Member and for the reviewer.
- **Return 404 for the missing Benefit.** This mixes up "no such Member" with "no rule on file", and clients would have to treat an ordinary answer as an error.

## Consequences

- Callers handle three statuses. The Copilot routes `NO_BENEFIT_ON_FILE` to a person rather than to a denial.
- An exclusion must be recorded as a Benefit row with `covered` false. Leaving a procedure out of a Plan's Benefits does not exclude it.
- FHIR has no status for this. In the [ADR 0001](0001-plain-json-loosely-following-fhir.md) mapping it is a response with no `item` for the procedure, which is easy to miss. The explicit status is part of why this API isn't FHIR.
