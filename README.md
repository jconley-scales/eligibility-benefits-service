# Eligibility & Benefits Service

A small Java REST service that answers an Eligibility check for one Member, one Procedure, and one Service date: is the Member's coverage active, what is their Plan's Benefit for the procedure, does it need prior authorization, and what is their cost share. Every Plan and Member is synthetic; there is no protected health information anywhere in this repo. Terms are defined in [`GLOSSARY.md`](GLOSSARY.md).

## API

The API reference is at **[jconley-scales.github.io/eligibility-benefits-service](https://jconley-scales.github.io/eligibility-benefits-service/)**, rendered by Redoc from the committed OpenAPI contract, [`docs/api/openapi.json`](docs/api/openapi.json). Build API clients against that file: the build fails whenever it differs from the API the code generates. A running service also serves the live document at `http://localhost:8080/v3/api-docs`.

### Changing the API

springdoc-openapi generates the contract from the code, and `ApiContractTest` fails when the committed file differs from it. After an intended API change, regenerate the file and commit it with the change:

```sh
./mvnw test -Dtest=ApiContractTest -DregenerateContract
```

The diff of `docs/api/openapi.json` is the API change as clients will see it, so review it before committing. Once the change is on `main`, the docs page republishes.

## Synthetic seed data

`docker compose up` starts the service with these Plans, Members, Coverages, and Benefits already loaded, so you can try any scenario right away. Each Member stands for one edge case:

```sh
curl http://localhost:8080/v1/members/SYN-0007/coverage
curl http://localhost:8080/v1/plans/HMO-STANDARD/benefits
```

### Members

The end date is the last covered day. An open end means the Coverage is ongoing.

| Member ID  | Edge case                                                                       | Coverage                                                                     |
| ---------- | ------------------------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| `SYN-0001` | Active coverage with no end date, part of the deductible met                    | `PPO-LOW-DED` from 2025-01-01, ongoing                                       |
| `SYN-0002` | Deductible fully met                                                            | `PPO-HIGH-DED` from 2026-01-01, ongoing                                      |
| `SYN-0003` | Out-of-pocket maximum met                                                       | `HMO-STANDARD` from 2024-01-01, ongoing                                      |
| `SYN-0004` | Coverage lapsed (ended in a previous year)                                      | `PPO-LOW-DED` 2023-01-01 to 2025-12-31                                       |
| `SYN-0005` | Coverage starting 2027-01-01                                                    | `HMO-STANDARD` from 2027-01-01, ongoing                                      |
| `SYN-0006` | Coverage ending 2026-12-31                                                      | `PPO-LOW-DED` 2026-01-01 to 2026-12-31                                       |
| `SYN-0007` | Plan switch mid-year                                                            | `PPO-HIGH-DED` 2026-01-01 to 2026-06-30, then `HMO-STANDARD` from 2026-07-01 |
| `SYN-0008` | No Accumulator for the service year                                             | `PPO-LOW-DED` from 2025-01-01, ongoing                                       |
| `SYN-0009` | Plan excludes the requested procedure: `95810` is not covered                   | `HMO-STANDARD` from 2026-01-01, ongoing                                      |
| `SYN-0010` | Plan has no Benefit for the requested procedure: `97110` has no benefit on file | `PPO-HIGH-DED` from 2026-01-01, ongoing                                      |

Benefits belong to the Plan, so any Member on `HMO-STANDARD` gets "not covered" for `95810`, and any Member on `PPO-HIGH-DED` gets "no benefit on file" for `97110`. `SYN-0009` and `SYN-0010` are the Members set aside for those two cases. The Accumulator edge cases (deductible and out-of-pocket totals, no Accumulator for the service year) take effect once that table is added. The Members and Coverages they attach to are already here.

### Plans

| Plan ID        | Name                          | Type | Deductible | Out-of-pocket maximum |
| -------------- | ----------------------------- | ---- | ---------: | --------------------: |
| `PPO-LOW-DED`  | Synthetic Low-Deductible PPO  | PPO  |    $500.00 |             $3,000.00 |
| `HMO-STANDARD` | Synthetic Standard HMO        | HMO  |    $250.00 |             $4,000.00 |
| `PPO-HIGH-DED` | Synthetic High-Deductible PPO | PPO  |  $3,000.00 |             $7,000.00 |

### Benefits

Each Plan has a Benefit for each of the five pilot procedures the Prior Authorization Copilot uses, except where marked. A cell gives the member's cost share, either a copay in dollars or a coinsurance percentage (the other is zero), then "deductible" if the deductible applies and "prior auth" if prior authorization is required.

| Procedure                     | `PPO-LOW-DED`               | `HMO-STANDARD`              | `PPO-HIGH-DED`              |
| ----------------------------- | --------------------------- | --------------------------- | --------------------------- |
| `29881` knee arthroscopy      | 20%, deductible, prior auth | $150.00, prior auth         | 30%, deductible, prior auth |
| `72148` lumbar spine MRI      | 20%, deductible             | $75.00, prior auth          | 30%, deductible, prior auth |
| `95810` sleep study           | 20%, deductible, prior auth | **Not covered**             | 30%, deductible, prior auth |
| `97110` physical therapy      | $30.00                      | $25.00                      | **No benefit on file**      |
| `E0601` CPAP device (HCPCS)   | 20%, deductible, prior auth | 10%, deductible, prior auth | 30%, deductible, prior auth |

"Not covered" means the Plan has a Benefit saying it doesn't pay for the procedure. "No benefit on file" means the Plan has no Benefit for it at all: the answer is unknown, not unpaid, so a person decides.

## Design decisions

- [ADR 0002](docs/adr/0002-postgres-with-database-enforced-coverage-rule.md): Postgres, with the one-coverage-at-a-time rule enforced by the database.
