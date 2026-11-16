# Eligibility & Benefits Service

A small Java REST service that answers an Eligibility check for one Member, one Procedure, and one Service date: is the Member's coverage active, what is their Plan's Benefit for the procedure, does it need prior authorization, and what is their cost share. Every Plan and Member is synthetic; there is no protected health information anywhere in this repo. Terms are defined in [`GLOSSARY.md`](GLOSSARY.md).

## Synthetic seed data

`docker compose up` starts the service with these Plans, Members, and Coverages already loaded, so you can try any scenario right away. Each Member stands for one edge case:

```sh
curl http://localhost:8080/v1/members/SYN-0007/coverage
```

### Members

The end date is the last covered day. An open end means the Coverage is ongoing.

| Member ID  | Edge case                                                    | Coverage                                                                     |
| ---------- | ------------------------------------------------------------ | ---------------------------------------------------------------------------- |
| `SYN-0001` | Active coverage with no end date, part of the deductible met | `PPO-LOW-DED` from 2025-01-01, ongoing                                       |
| `SYN-0002` | Deductible fully met                                         | `PPO-HIGH-DED` from 2026-01-01, ongoing                                      |
| `SYN-0003` | Out-of-pocket maximum met                                    | `HMO-STANDARD` from 2024-01-01, ongoing                                      |
| `SYN-0004` | Coverage lapsed (ended in a previous year)                   | `PPO-LOW-DED` 2023-01-01 to 2025-12-31                                       |
| `SYN-0005` | Coverage starting 2027-01-01                                 | `HMO-STANDARD` from 2027-01-01, ongoing                                      |
| `SYN-0006` | Coverage ending 2026-12-31                                   | `PPO-LOW-DED` 2026-01-01 to 2026-12-31                                       |
| `SYN-0007` | Plan switch mid-year                                         | `PPO-HIGH-DED` 2026-01-01 to 2026-06-30, then `HMO-STANDARD` from 2026-07-01 |
| `SYN-0008` | No Accumulator for the service year                          | `PPO-LOW-DED` from 2025-01-01, ongoing                                       |
| `SYN-0009` | Plan excludes the requested procedure                        | `HMO-STANDARD` from 2026-01-01, ongoing                                      |
| `SYN-0010` | Plan has no Benefit for the requested procedure              | `PPO-HIGH-DED` from 2026-01-01, ongoing                                      |

The Benefit and Accumulator edge cases (deductible and out-of-pocket totals, excluded procedures, no Benefit on file) take effect once those tables are added. The Members and Coverages they attach to are already here.

### Plans

| Plan ID        | Name                          | Type | Deductible | Out-of-pocket maximum |
| -------------- | ----------------------------- | ---- | ---------: | --------------------: |
| `PPO-LOW-DED`  | Synthetic Low-Deductible PPO  | PPO  |    $500.00 |             $3,000.00 |
| `HMO-STANDARD` | Synthetic Standard HMO        | HMO  |    $250.00 |             $4,000.00 |
| `PPO-HIGH-DED` | Synthetic High-Deductible PPO | PPO  |  $3,000.00 |             $7,000.00 |

## Design decisions

- [ADR 0002](docs/adr/0002-postgres-with-database-enforced-coverage-rule.md): Postgres, with the one-coverage-at-a-time rule enforced by the database.
