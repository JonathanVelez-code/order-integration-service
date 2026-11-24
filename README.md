# Enterprise Order Integration Service

A Spring Boot service that keeps orders consistent between an **ERP** (the system of record) and a **CRM**:

- **Every 15 minutes**, it pulls changed orders from the ERP, saves them in a transaction, and pushes them to the CRM, retrying temporary failures.
- **Every night**, a **Spring Batch** job compares every ERP order with the CRM, records each difference, and repairs the CRM.
- **Every change** is written to an audit log, in the same transaction as the change itself.

![CI](https://github.com/JonathanVelez-code/order-integration-service/actions/workflows/ci.yml/badge.svg)

> **Note:** the ERP and CRM are simulated in memory (`external/simulated`), behind the `ErpClient` and `CrmClient` interfaces. A real deployment would swap in REST or JDBC clients without touching the rest of the code. The simulated CRM can be told to fail, to show the retry and recovery paths.

**Stack:** Java 17, Spring Boot 3.3, Spring Data JPA (Hibernate), Spring Batch 5, Spring Retry, H2 / PostgreSQL, JUnit 5, Mockito, Gradle (Groovy DSL), Docker, GitHub Actions

---

## How it works

```
                 every 15 min                                        nightly, 2:00 AM
ERP ──changed orders──▶ OrderSyncService                ERP ──all orders──▶ reconciliationJob (Spring Batch)
                            │                                                   │
              ┌─────────────┴─────────────┐                 Step 1 findDiscrepancies (chunks of 50)
              ▼                           ▼                   read ERP → compare with CRM → save differences
   OrderUpsertService             CrmGateway (@Retryable)       CRM still down? skip the order + audit it
   one @Transactional per order   3 attempts, exponential               │
   order + audit line together    backoff                       Step 2 repairCrm (tasklet)
              │                           │                     push the ERP version for each difference
              ▼                           ▼                             │
         database                       CRM                              ▼
  (synced_order, audit_entry,     fails after retries?              CRM matches ERP
   sync_state, discrepancy)       → order marked PENDING,
                                    fixed by the nightly job
```

### Design decisions

| Decision | Why |
|---|---|
| One transaction **per order**, not one for the whole sync | One bad order doesn't roll back the others. |
| The order change and its **audit line share a transaction** | The audit log can never miss a change, or show one that didn't happen. A test checks the rollback. |
| CRM calls happen **outside** the database transaction | A slow CRM never holds database locks. |
| **Retries with exponential backoff** (`@Retryable`, Spring AOP) | Most CRM failures are temporary (timeouts, restarts). |
| Failed pushes become **PENDING**; the nightly job repairs them | The sync never blocks on the CRM, and nothing is lost. |
| A **watermark**, saved only at the end of a sync | A crash halfway means the next run re-processes those orders. That's safe because saving and pushing are idempotent. |
| Batch **skip policy** with an audit entry | One unreachable order doesn't fail the whole nightly run. |
| `@StepScope` beans | Each run gets its own writer and tasklet, which know the run's job execution ID. |
| `BigDecimal.compareTo`, not `equals` | `equals` says 10.0 ≠ 10.00, which would report fake mismatches. |
| `@Version` on orders | Optimistic locking: two writers can't silently overwrite each other. |

---

## API

| Method | Path | What it does |
|---|---|---|
| POST | `/api/sync` | Run the ERP → CRM sync now |
| GET | `/api/orders?crmStatus=PENDING` | Synced orders (optionally only those not yet in the CRM) |
| POST | `/api/reconciliations` | Run the reconciliation job now |
| GET | `/api/reconciliations/{jobExecutionId}/discrepancies` | What a run found and fixed |
| GET | `/api/audit?orderId=ORD-00028` | The audit log (latest 100, or one order's history) |
| GET | `/actuator/health` | Health check |

Example run with the built-in demo data (200 orders; the CRM copy starts with 10 missing and 8 stale orders):

```
POST /api/reconciliations  → {"ordersChecked":200,"discrepanciesFound":18,"discrepanciesResolved":18,"ordersSkipped":0}
POST /api/sync             → {"changedInErp":200,"created":200,"pushedToCrm":200,"pendingInCrm":0, ...}
POST /api/reconciliations  → {"ordersChecked":200,"discrepanciesFound":0, ...}
```

---

## Run it

**Locally, with an in-memory H2 database** (needs Java 17+):

```bash
./gradlew bootRun          # Windows: gradlew.bat bootRun
curl -X POST localhost:8080/api/reconciliations
```

**With Docker and PostgreSQL:**

```bash
docker compose up --build
```

**Tests:**

```bash
./gradlew test             # report: build/reports/tests/test/index.html
```

---

## Tests

18 JUnit 5 tests; they run on every push in GitHub Actions, which also builds the Docker image.

| Test class | What it covers |
|---|---|
| `OrderComparatorTest` | Unit tests of the comparison rules, including `BigDecimal` scale |
| `OrderSyncServiceTest` | New orders, watermark, unchanged orders, **retries**, PENDING when the CRM stays down |
| `OrderUpsertRollbackTest` | **Transaction rollback**: if the audit write fails, the order isn't saved |
| `ReconciliationJobTest` | The real Spring Batch job end to end: every kind of drift, repair, **skip policy**, retries |
| `IntegrationControllerTest` | REST layer (`@WebMvcTest`) with mocked services |

---

## Project structure

```
src/main/java/com/jonathanvelez/orderintegration/
├── external/            ErpClient, CrmClient, the order records, and the simulated systems
├── domain/              JPA entities and Spring Data repositories
├── audit/               AuditService
├── sync/                OrderSyncService, OrderUpsertService, CrmGateway (retries)
├── reconcile/           Spring Batch job: reader, processor, writer, skip listener, repair tasklet
├── schedule/            @Scheduled sync (15 min) and nightly reconciliation
├── web/                 REST controller
└── demo/                Demo data seeder
```

## Next steps

- Database migrations with Flyway instead of Hibernate `ddl-auto`
- Integration tests against real PostgreSQL with Testcontainers
- An outbox table so CRM pushes survive an app crash between the save and the push
- Real ERP/CRM clients with WireMock contract tests
