# Eligibility & Benefits

Answers, for one member, one procedure, and one service date: is the member's coverage active, what is their plan's benefit for that procedure, does it need prior authorization, and what is the member's cost share. All members and plans are synthetic.

## Language

### People and plans

**Member**:
A person enrolled with the insurer. One person is one member; there is no subscriber or dependent distinction.
_Avoid_: Patient (the provider's word for the same person), subscriber, enrollee, beneficiary

**Plan**:
A health insurance product with a plan type, a deductible, and an out-of-pocket maximum.
_Avoid_: Policy, product

**Plan type**:
Whether a plan is an HMO (health maintenance organization) or a PPO (preferred provider organization), as a label only.
_Avoid_: Product type, network type

**Coverage**:
A member's enrollment in one plan for a date range. A member has at most one coverage on any date; an open end means ongoing, and the end date is the last covered day.
_Avoid_: Covered (that is a Benefit question), enrollment, policy

**Active coverage**:
The member's coverage whose date range includes the service date. A member has no active coverage on a date before their coverage starts or after it ends.
_Avoid_: In force, effective, eligible

### Procedures and benefits

**Procedure**:
A medical service identified by a CPT or HCPCS code.
_Avoid_: Service, treatment

**Benefit**:
A plan's rule for one procedure: whether the plan pays for it and, when it does, on what terms: whether it needs prior authorization, whether the deductible applies to it, and the member's cost share for it. A Benefit for a procedure the plan doesn't pay for (not covered) has no terms.
_Avoid_: Coverage, "covered" as a name for the Benefit itself

**Covered**:
A procedure the plan pays for, as its Benefit says.
_Avoid_: Approved, authorized, eligible

**Not covered**:
A procedure the plan excludes: it has a Benefit saying the plan doesn't pay for it. It is the plan's own answer, unlike no benefit on file.
_Avoid_: Denied, rejected

**No benefit on file**:
The plan has no Benefit for the requested procedure. It means unknown, not unpaid, so a person decides.
_Avoid_: Not covered, denied

**Prior authorization**:
The insurer's approval, required for some procedures under some plans, before the procedure is performed.
_Avoid_: Pre-authorization, precertification, pre-approval

### Money

**Cost share**:
The part of a procedure's cost the member pays, made up of copay, coinsurance, and deductible.
_Avoid_: Out-of-pocket cost, member responsibility

**Copay**:
A fixed dollar amount the member pays for a procedure.

**Coinsurance**:
The percentage of a procedure's cost the member pays.

**Deductible**:
The amount a member pays in a plan year before the plan starts paying.

**Out-of-pocket maximum**:
The most a member pays in a plan year; after that the plan pays everything.
_Avoid_: OOP cap, stop-loss

**Accumulator**:
A member's running totals of deductible and out-of-pocket spending under one coverage in one plan year. Switching plans mid-year starts new totals at zero.
_Avoid_: Balance, spend

**Deductible remaining**:
What is left of the plan's deductible under one coverage in one plan year: the deductible minus what the Accumulator records as met, never below zero.
_Avoid_: Balance, deductible left

**Out-of-pocket remaining**:
What is left of the plan's out-of-pocket maximum under one coverage in one plan year: the maximum minus what the Accumulator records as met, never below zero.
_Avoid_: Balance, OOP left

**Plan year**:
The calendar year, January 1 to December 31, on which deductibles and out-of-pocket maximums reset.
_Avoid_: Benefit year, policy year

### Checking eligibility

**Service date**:
The date a procedure is or was performed. Every answer is judged as of this date, past or future.
_Avoid_: Date of service, request date, today

**Eligibility check**:
One question about one member, one procedure, and one service date: is coverage active, what is the Benefit, is prior authorization required, and what is the cost share.
_Avoid_: Verification, benefits check

### Not modeled

**Coordination of benefits**:
Deciding which of a member's two simultaneous coverages pays first. Not modeled: a member has one coverage at a time.
_Avoid_: COB

**Network**:
The providers a plan has contracts with, which changes cost share and, for an HMO, whether a referral is needed. Not modeled: plan type is a label only.
_Avoid_: In-network / out-of-network rules, referral rules
