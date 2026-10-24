# PaperERP Enterprise

> **Industrial Paper Manufacturing ERP Platform**  
> An industrial-grade, configurable ERP platform built specifically for paper mills, featuring individual paper-roll traceability, parent-child slitting genealogy, an immutable dual-entry inventory movement ledger, strict quality inspection gates, multi-tenant composite isolation, and an Arabic-first right-to-left (RTL) experience.

---

## 1. Quick Access: Local LAN & Remote Cloudflare

### 1.1 Local Factory Network Access (LAN)
The platform is bound to `0.0.0.0` so any device (PC, laptop, floor tablet) on the factory network can open the ERP using the local IP and Port:
* **Frontend UI (Operators & Management)**: [http://192.168.9.223:5173](http://192.168.9.223:5173)
* **Backend API (Spring Boot Health & REST)**: [http://192.168.9.223:8080/api/v1/health](http://192.168.9.223:8080/api/v1/health)

To start both servers with one click on the host server:
```powershell
.\start_server_lan.bat
```

### 1.3 Automatic Cloudflare Pages Deployment on Every Git Push
The repository contains `frontend/public/_redirects` and `frontend/public/_headers` ready for Cloudflare Pages:
1. Go to your **[Cloudflare Dashboard](https://dash.cloudflare.com/)** -> **Workers & Pages** -> **Create application** -> **Pages** -> **Connect to Git**.
2. Select repository: `negm2508-byte/Parmagitna`.
3. Set the build configuration:
   * **Framework preset**: `Vite`
   * **Root directory**: `frontend`
   * **Build command**: `npm run build`
   * **Output directory**: `dist`
4. Click **Save and Deploy**.  
Every time we push to GitHub (`main`), Cloudflare automatically builds and updates the live URL (e.g. `https://papererp.pages.dev`) for your client!

---

## 2. Technical Stack & Architecture

| Layer | Technology | Approved Version | Notes |
| :--- | :--- | :--- | :--- |
| **Backend Runtime** | Java OpenJDK (LTS) | 17 LTS | Tested on OpenJDK 17.0.20.1 Microsoft Hotspot |
| **Backend Framework** | Spring Boot | 3.4.2 | Modular Monolith architecture |
| **Persistence / ORM** | Spring Data JPA / Hibernate | 6.4+ / 3.4.2 | Strict check constraints, non-negative balances |
| **Database Migrations** | Flyway | 10.20.1 | `V1__initial_schema.sql` and `V2__corrected_kernel_schema.sql` |
| **Database Engine** | PostgreSQL | 16+ | Zero-trust multi-tenancy via composite foreign keys |
| **Frontend Framework** | React + TypeScript | 19.0.0 / TS 5.7+ | Built with Vite 6 (Binds to `0.0.0.0`) |
| **Frontend Styling** | Tailwind / Cairo Font | Logical RTL | Interactive knife deckle calculator & genealogy tree |
| **Thermal Printing** | Zebra/TSC 100x150 | EAN-128 / Code 128 | Instant printable thermal barcode labels |
| **Cloud Integration** | Cloudflare Tunnel & Pages | Latest (2026.10+) | Live client demo & automated git push deployment |

---

## 3. Verified Manufacturing Kernel Capabilities

### 3.1 Composite Multi-Tenancy Isolation
* Every foreign key enforces `(tenant_id, id)`.
* Verified by automated tests: An operation from Tenant B attempting to reference Plant A or Machine A from Tenant A is strictly rejected by the PostgreSQL engine.

### 3.2 Atomic Slitting Execution & Mass Balance Reconciliation
* In a single `@Transactional` ACID execution:
  - Parent Jumbo Roll (Mother roll) transitions to `FULLY_SLIT`.
  - A `slitting_runs` batch record is created.
  - Child slit rolls are created with parent genealogy and unique serial numbers.
  - A `STATUS_CHANGE` movement is recorded for the Jumbo Roll.
  - A `PRODUCTION_RECEIPT` movement is recorded for each child roll.
  - Mass balance invariant is verified with strict 1.0% tolerance limit:
    $$\text{Jumbo Net Weight} = \sum \text{Child Net Weights} + \text{Trim Waste Kg} \pm \text{Scale Variance}$$

### 3.3 Atomic Rollback on Failure
* If a slitting run fails (e.g. cut widths exceed parent deckle width, variance exceeds 1.0%, or an unexpected exception occurs), the entire transaction rolls back completely:
  - Parent Jumbo Roll remains `ACTIVE_WIP`.
  - Exactly zero orphaned child rolls are created.
  - Exactly zero unlinked movements exist in the ledger.

---

## 4. Automated Testing & Verification Commands

```powershell
# 1. Run all Backend tests (Spring Boot, Flyway V1/V2, Kernel Integration, REST API)
cd backend
.\mvnw.cmd test             # 11 tests run, 0 failures, 0 errors (100% PASS)

# 2. Run all Frontend tests (Vitest component tests)
cd ..\frontend
npm test                    # 3 tests run, 0 failures, 0 errors (100% PASS)

# 3. Verify Frontend Production Bundle
npm run build               # Builds cleanly in ~3.3s
```
