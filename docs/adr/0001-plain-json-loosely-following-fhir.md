# Plain JSON loosely following FHIR, not full FHIR or X12 270/271

The Eligibility check has one caller, the Prior Authorization Copilot, and it needs one answer to four questions: is coverage active on the Service date, what is the Plan's Benefit for the procedure, is prior authorization required, and what is the cost share. Healthcare already has two standard formats for this exchange. X12 270/271 is the EDI inquiry and response that providers and payers send each other. HL7 FHIR R4 has `CoverageEligibilityRequest` and `CoverageEligibilityResponse` resources.

We use neither in full. `POST /v1/eligibility/check` takes and returns plain JSON whose shape loosely follows FHIR, with field names from the glossary. The mapping is recorded here, so a FHIR front end later would be a translation rather than a redesign:

| This API | FHIR R4 |
| --- | --- |
| request `memberId` | `CoverageEligibilityRequest.patient`, referenced by identifier (`patient.identifier`) |
| request `procedureCode` | `CoverageEligibilityRequest.item.productOrService`, coded as CPT or HCPCS |
| request `serviceDate` | `CoverageEligibilityRequest.servicedDate` |
| `coverage.active` | `CoverageEligibilityResponse.insurance.inforce` |
| `coverage.planId`, `planName`, `planType` | `insurance.coverage`, a reference to a `Coverage` resource naming the plan |
| `coverage.planYear` | `insurance.benefitPeriod` |
| `benefit.status` of `NOT_COVERED` | `insurance.item.excluded` set to true |
| `benefit.status` of `NO_BENEFIT_ON_FILE` | no `insurance.item` for the procedure ([ADR 0003](0003-no-benefit-on-file-is-not-a-denial.md)) |
| `benefit.priorAuthRequired` | `insurance.item.authorizationRequired` |
| `benefit.copay` | an `insurance.item.benefit` entry of type `copay`, with `allowedMoney` |
| `benefit.coinsurancePercent` | an `insurance.item.benefit` entry of type `copay-percent`, with `allowedUnsignedInt` |
| `benefit.deductibleApplies` | a `deductible` entry in `insurance.item.benefit`; FHIR has no flag for it |
| `coverage.deductibleRemaining` | a `deductible` entry in `insurance.item.benefit`: `allowedMoney` minus `usedMoney` |
| `coverage.outOfPocketRemaining` | an `insurance.item.benefit` entry for the out-of-pocket maximum: `allowedMoney` minus `usedMoney` |

FHIR reports what is allowed and what is used, while this API reports only what remains, never below zero. FHIR puts these entries on an `item`; this API puts them on `coverage` because they don't depend on the procedure.

## Considered options

- **Full FHIR R4.** Conformance means resources with references, `CodeableConcept` codings, and usually a profile such as Da Vinci's, plus validation against it. For one caller asking one question, most of that structure would be empty, and the four answers would be buried in it.
- **X12 270/271.** It is a positional EDI format with coded segments, built for clearinghouses and batch exchange. The Copilot is a JSON-over-HTTP client and would need a translator just to call the service.
- **Plain JSON with no reference to either standard.** This is simplest today, but readers lose the link to the vocabulary the industry uses, and a later move to FHIR would mean working out the mapping from scratch.

## Consequences

- The API is not FHIR-conformant, and a FHIR client can't call it directly. The table above is the translation.
- Field names follow the glossary rather than FHIR: `memberId`, not `patient`, and `NOT_COVERED`, not `excluded`.
- One explicit `status` stands in for FHIR's `excluded` flag and missing item, so "not covered" and "no benefit on file" can't be confused.
- Units are fixed instead of carried in each value: money is a JSON number in dollars with two decimals, where FHIR's `Money` carries a currency, and coinsurance is a whole-number percentage from 0 to 100.
