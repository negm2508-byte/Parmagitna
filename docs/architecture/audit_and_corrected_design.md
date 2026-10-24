# PaperERP Enterprise — Critical Architecture Audit & Corrected Design Blueprint

> **Auditor**: Principal Enterprise Software Architect & Industrial Systems Engineer  
> **Target System**: PaperERP Enterprise (Multi-Tenant Paper Manufacturing ERP)  
> **Scope**: Schema Integrity, Inventory Ledger, Multi-Tenancy Isolation, Atomic Slitting, Security & Operational Correctness  
> **Status**: PENDING CLIENT APPROVAL (Implementation blocked until design sign-off)  

---

## 1. Prioritized List of Design Defects & Vulnerabilities

A rigorous audit of the baseline schema (`V1__initial_schema.sql`) and architectural blueprint reveals six critical flaws that must be corrected before writing production business code.

---

### [DEFECT-01] CRITICAL: Inventory Movement Ledger Ignores Jumbo Rolls (Mother Rolls)
* **Classification**: **Confirmed Architectural Defect**
* **Finding**: In `V1__initial_schema.sql`, `inventory_movements` defines:
  ```sql
  roll_id UUID NOT NULL REFERENCES slit_rolls(id) ON DELETE RESTRICT
  ```
* **Failure Mode**:
  1. A paper mill produces **Jumbo Rolls (Mother Rolls / Pope Reel)** off the Paper Machine before slitting.
  2. Jumbo rolls represent 80%+ of WIP (Work-In-Process) capital. They are weighed, moved to drying areas, transferred to slitter buffers, or occasionally scrapped directly if defective.
  3. Because `inventory_movements` strictly references `slit_rolls`, **the system cannot record the inventory receipt, location transfer, or consumption of Jumbo Rolls**.
* **Impact**: Total lack of WIP inventory visibility; broken mass balance reconciliation; impossible to track jumbo roll storage locations in warehouses.
* **Proposed Correction**:
  Make `inventory_movements` support both roll types while preserving strict relational integrity via dual nullable foreign keys and a `CHECK` constraint:
  ```sql
  jumbo_roll_id UUID REFERENCES jumbo_rolls(id) ON DELETE RESTRICT,
  slit_roll_id UUID REFERENCES slit_rolls(id) ON DELETE RESTRICT,
  CONSTRAINT chk_movement_roll_target CHECK (
      (jumbo_roll_id IS NOT NULL AND slit_roll_id IS NULL) OR
      (jumbo_roll_id IS NULL AND slit_roll_id IS NOT NULL)
  )
  ```

---

### [DEFECT-02] CRITICAL: Cross-Tenant Data Corruption via Simple Foreign Keys
* **Classification**: **Confirmed Security & Isolation Defect**
* **Finding**: Operational tables (`users`, `machines`, `jumbo_rolls`, `slit_rolls`, `inventory_movements`) have their own `tenant_id` column, but foreign keys reference only the target entity's `id`:
  ```sql
  -- Example in jumbo_rolls:
  tenant_id UUID NOT NULL REFERENCES tenants(id),
  plant_id UUID NOT NULL REFERENCES plants(id),
  machine_id UUID NOT NULL REFERENCES machines(id),
  paper_grade_id UUID NOT NULL REFERENCES paper_grades(id),
  ```
* **Failure Mode**:
  PostgreSQL simple foreign keys check only that the target `id` exists in the referenced table. They **do NOT** check that `target.tenant_id == source.tenant_id`.
  An API bug or malicious request from Tenant A can insert a Jumbo Roll with `tenant_id = Tenant_A` and `machine_id = Machine_B` (belonging to Tenant B). PostgreSQL will happily accept this transaction!
* **Impact**: Silent cross-tenant data leaks and relational corruption across multi-tenant factories.
* **Proposed Correction**:
  Enforce **Composite Foreign Keys** `(tenant_id, id)` across all relational hierarchies:
  ```sql
  -- All parent tables must expose a composite unique constraint:
  CONSTRAINT uq_plants_tenant_id UNIQUE (tenant_id, id)
  CONSTRAINT uq_machines_tenant_id UNIQUE (tenant_id, id)
  CONSTRAINT uq_paper_grades_tenant_id UNIQUE (tenant_id, id)

  -- All referencing tables must enforce composite foreign keys:
  CONSTRAINT fk_jumbo_plant FOREIGN KEY (tenant_id, plant_id) REFERENCES plants(tenant_id, id),
  CONSTRAINT fk_jumbo_machine FOREIGN KEY (tenant_id, machine_id) REFERENCES machines(tenant_id, id),
  CONSTRAINT fk_jumbo_grade FOREIGN KEY (tenant_id, paper_grade_id) REFERENCES paper_grades(tenant_id, id)
  ```
  This guarantees at the database storage engine level that cross-tenant references are physically impossible.

---

### [DEFECT-03] HIGH: Missing `slitting_runs` Aggregate Entity & Mass Balance Gap
* **Classification**: **Confirmed Manufacturing Domain Defect**
* **Finding**: Slit rolls currently only point to `parent_jumbo_id`. There is no first-class operational entity representing the **Slitting Run (Operation Batch)** itself.
* **Failure Mode**:
  When a Jumbo Roll (e.g., 2,400 mm, 3,500 kg) is slit into 3 child rolls:
  - Slitting generates physical **Edge Trim Waste** (e.g., 50 mm, 75 kg) and sawdust/shavings.
  - Core weight (tare) is added with each cardboard core.
  - Scale measurement variance ($\pm 0.5\%$) occurs.
  Without a `slitting_runs` entity:
  1. We cannot record how much trim waste in kg was produced and recycled to the pulper.
  2. We cannot record machine downtime, slitter speed, blade wear, or shift attribution for the slitting operation.
  3. We cannot mathematically verify the mill mass balance formula:
     $$\text{Jumbo Net Weight} = \sum \text{Child Net Weights} + \text{Recorded Trim Waste Kg} + \text{Variance Kg}$$
* **Proposed Correction**:
  Introduce a formal `slitting_runs` table:
  ```sql
  CREATE TABLE slitting_runs (
      id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
      tenant_id UUID NOT NULL REFERENCES tenants(id),
      parent_jumbo_id UUID NOT NULL,
      machine_id UUID NOT NULL,
      operator_id UUID NOT NULL,
      input_jumbo_weight_kg NUMERIC(10,2) NOT NULL,
      total_child_weight_kg NUMERIC(10,2) NOT NULL,
      trim_waste_kg NUMERIC(10,2) NOT NULL,
      scale_variance_kg NUMERIC(10,2) NOT NULL,
      started_at TIMESTAMP WITH TIME ZONE NOT NULL,
      completed_at TIMESTAMP WITH TIME ZONE NOT NULL,
      status VARCHAR(30) NOT NULL CHECK (status IN ('COMPLETED', 'ABORTED')),
      CONSTRAINT fk_slitting_jumbo FOREIGN KEY (tenant_id, parent_jumbo_id) REFERENCES jumbo_rolls(tenant_id, id),
      CONSTRAINT fk_slitting_machine FOREIGN KEY (tenant_id, machine_id) REFERENCES machines(tenant_id, id)
  );
  ```
  And link `slit_rolls` to `slitting_run_id` as well as `parent_jumbo_id`.

---

### [DEFECT-04] HIGH: PostgreSQL Row-Level Security (RLS) Leak Hazard with HikariCP
* **Classification**: **Operational Security Risk**
* **Finding**: The plan calls for PostgreSQL Row-Level Security (RLS) using session variables (`SET LOCAL app.current_tenant_id = ?`).
* **Failure Mode**:
  1. Connection pooling (HikariCP) keeps physical PostgreSQL connections alive across hundreds of HTTP requests.
  2. `SET LOCAL` is only scoped to a transaction block (`BEGIN ... COMMIT`). If any code runs outside an explicit `@Transactional` boundary, or if a transaction fails before `SET LOCAL` is cleared, the session variable persists on that connection.
  3. A subsequent request from Tenant B borrowing that connection from the pool will execute with Tenant A's context, causing massive silent data leaks!
* **Proposed Correction**:
  1. Primary tenant defense: Spring Data JPA `@TenantId` / Hibernate Filter automatically appends `WHERE tenant_id = ?` to every query, independent of database connection state.
  2. Database defense-in-depth: RLS must be paired with an interceptor or HikariCP connection wrapper that executes `RESET ALL` upon connection release back to the pool.
  3. Strict integration test: Write a test that borrows the same database connection across two simulated tenant requests and asserts zero data crossover.

---

### [DEFECT-05] MEDIUM: Cookie-Based JWT CSRF Vulnerability & Revocation Gap
* **Classification**: **Security Defect**
* **Finding**: Storing stateless JWTs in `HttpOnly` cookies without CSRF protection leaves factory web clients vulnerable to Cross-Site Request Forgery (CSRF). Furthermore, purely stateless JWTs cannot be revoked immediately if a plant terminal is compromised.
* **Proposed Correction**:
  1. **CSRF Token**: Enable Spring Security's `CookieCsrfTokenRepository.withHttpOnlyFalse()`. The browser reads the `XSRF-TOKEN` cookie and must include it in the `X-XSRF-TOKEN` header on all mutating requests (`POST`, `PUT`, `DELETE`).
  2. **Short-Lived Access + Database-Backed Refresh Token**:
     - Access JWT: 15 minutes lifetime, stored in memory or secure cookie.
     - Refresh Token: 8 hours (1 factory shift), stored hashed in a database `refresh_tokens` table. Allows instant revocation from the admin panel if an operator's tablet is lost.

---

### [DEFECT-06] MEDIUM: Dangerous Hardcoded Mill Defaults in Database DDL
* **Classification**: **Domain Integrity Defect**
* **Finding**: `V1__initial_schema.sql` hardcoded:
  ```sql
  core_diameter_mm NUMERIC(6,2) NOT NULL DEFAULT 76.0 CHECK (core_diameter_mm > 0),
  tare_weight_kg NUMERIC(10,2) NOT NULL DEFAULT 2.0 CHECK (tare_weight_kg >= 0),
  ```
* **Failure Mode**:
  In paper manufacturing:
  - Heavy board or testliner uses 150 mm (6-inch) steel-tipped cores. Light tissue uses 76 mm (3-inch) cores.
  - A 2,400 mm wide core can weigh 8.5 kg, while a 400 mm core weighs 1.2 kg.
  - A fixed `DEFAULT 2.0` injects fabricated tare weights into customer billing, leading to financial disputes.
* **Proposed Correction**:
  Remove arbitrary hardcoded numerical defaults from DDL. Require `core_diameter_mm` and `tare_weight_kg` to be explicitly provided by the operator or calculated from a verified core specification table.

---

## 2. Unresolved Manufacturing Domain Questions

Before finalizing production business rules in Sprints 2–5, the following questions require confirmation from factory management:

1. **Trim Waste Disposition**: Is edge trim from the slitter rewinder pneumatically blown directly back to the hydrapulper as wet broke, or weighed as baled dry broke?
2. **Moisture Weight Adjustment**: Does commercial sales invoicing use **actual scale weight** or normalized bone-dry weight adjusted for 7% standard equilibrium moisture?
3. **Core Tare Policy**: Is tare weight entered manually from the scale tare button, or looked up automatically based on core width $\times$ core grammage?
4. **Splice & Web-Break Policy**: Can customer slit rolls contain paper machine splices, or does a web break force the roll to be scrapped into broke?

---

## 3. Proposal: The Smallest Verifiable Proof Milestone (Milestone 1 — The Manufacturing Kernel)

Rather than building screens or full authentication, we propose implementing **Milestone 1: The Manufacturing Kernel** as an isolated, fully automated verification slice.

### Milestone 1 Objective
Prove with 100% automated test evidence that:
1. The backend starts and executes the corrected PostgreSQL schema migration.
2. Two test tenants (`TENANT_A` and `TENANT_B`) have 100% data isolation enforced by composite foreign keys and tenant filters.
3. A Jumbo Roll can be manufactured and decomposed into 3 Child Slit Rolls in **one atomic database transaction** (`@Transactional`).
4. All corresponding double-entry inventory movements (`PRODUCTION_RECEIPT` for Jumbo $\rightarrow$ `CONSUMPTION` for Jumbo $\rightarrow$ `PRODUCTION_RECEIPT` for Child Rolls) are generated consistently.
5. Mass balance is verified: $\text{Jumbo Net} = \sum \text{Child Net} + \text{Trim Waste}$.
6. Any failure (e.g. trim width mismatch or simulated exception) rolls back the entire transaction, leaving **zero partial rolls, zero orphaned movements, and zero ledger drift**.

### Measurable Acceptance Criteria (Given / When / Then)

#### Scenario 1: Strict Multi-Tenant Isolation
```gherkin
Given Tenant "TENANT_A" and Tenant "TENANT_B" exist
And Plant "PLANT_A" belongs to "TENANT_A"
When an operation attempts to create a machine for "TENANT_B" referencing "PLANT_A"
Then the database rejects the insertion with a composite foreign key violation
And querying machines as "TENANT_B" returns zero records from "TENANT_A"
```

#### Scenario 2: Atomic Slitting Execution with Mass Balance
```gherkin
Given an active Jumbo Roll belonging to "TENANT_A" with width 2400 mm and net weight 3000 kg
When the slitter operator executes a slitting run cutting:
  | Cut Seq | Width (mm) | Net Weight (kg) |
  | 1       | 1000       | 1245.0          |
  | 2       | 1000       | 1245.0          |
  | 3       |  350       |  435.0          |
And trim waste is recorded as 50 mm width and 65.0 kg
And scale variance is recorded as 10.0 kg
Then in one atomic transaction:
  - Parent Jumbo Roll transitions to "FULLY_SLIT"
  - A slitting_run record is created linking parent to children
  - Exactly 3 child slit_rolls are created with parent genealogy
  - A CONSUMPTION inventory movement is appended for the Jumbo Roll
  - Exactly 3 PRODUCTION_RECEIPT inventory movements are appended for the child rolls
  - All movements are timestamped, attributed, and verified in the ledger
```

#### Scenario 3: Atomic Rollback on Failure (Zero Partial Changes)
```gherkin
Given an active Jumbo Roll of 2400 mm width
When a slitting operation is submitted with cut widths summing to 2500 mm (exceeding deckle width)
Or when an unexpected database exception occurs halfway through child roll generation
Then the entire transaction rolls back
And the parent Jumbo Roll remains in state "ACTIVE_WIP"
And exactly zero child rolls exist in the database
And exactly zero inventory movements exist in the ledger
```

---

## 4. Next Step: Client Review & Approval Gate

In accordance with our strict governance rules:
* **No business logic code will be committed until this audit and the Milestone 1 plan are approved.**
* Upon your approval, we will create the corrected migration `V2__corrected_kernel_schema.sql`, implement the atomic `SlittingKernelService`, and deliver the automated integration test suite proving all three scenarios.
