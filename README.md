# PaperERP Enterprise

> **Industrial Paper Manufacturing ERP Platform**  
> An industrial-grade, configurable ERP platform built specifically for paper mills, featuring individual paper-roll traceability, parent-child slitting genealogy, an immutable inventory movement ledger, strict quality inspection gates, and an Arabic-first right-to-left (RTL) experience.

---

## 1. Technical Stack & Prerequisites

| Layer | Technology | Approved Version | Notes |
| :--- | :--- | :--- | :--- |
| **Backend Runtime** | Java OpenJDK (LTS) | 17 LTS (or 21 LTS) | Tested on OpenJDK 17.0.20.1 Microsoft Hotspot |
| **Backend Framework** | Spring Boot | 3.4.2 | Modular Monolith architecture |
| **Persistence / ORM** | Spring Data JPA / Hibernate | 6.4+ / 3.4.2 | Strict check constraints, non-negative balances |
| **Database Migrations** | Flyway | 10.20.1 | Version-controlled schema in `V1__initial_schema.sql` |
| **Database Engine** | PostgreSQL | 16+ | Docker container or native on-premises instance |
| **Frontend Framework** | React + TypeScript | 19.0.0 / TS 5.7+ | Built with Vite 6 |
| **Frontend Styling** | Tailwind / Cairo Font | Logical RTL | Native Arabic RTL typography and high-density layouts |
| **Package Managers** | Maven Wrapper / npm | Maven 3.9.9 / npm 11+ | Zero global Maven install required (`mvnw`) |

---

## 2. Repository Layout

```
PaperERP/
├── .github/
│   ├── workflows/
│   │   ├── ci-backend.yml              # CI: Maven verify, Flyway, Test execution
│   │   └── ci-frontend.yml             # CI: ESLint, TypeScript, Vitest, Vite build
│   ├── ISSUE_TEMPLATE/                 # Feature and bug issue templates
│   ├── PULL_REQUEST_TEMPLATE.md        # Enforced PR review checklist
│   └── CODEOWNERS                      # Dev A, B, C ownership boundaries
├── backend/
│   ├── mvnw / mvnw.cmd                 # Self-contained Maven Wrapper
│   ├── .mvn/wrapper/                   # Maven wrapper configuration
│   ├── pom.xml                         # Root parent POM (Spring Boot 3.4.2)
│   ├── paper-erp-common/               # Shared domain primitives, exceptions, TenantContext
│   ├── paper-erp-core/                 # JPA entities, movement ledger, Flyway migrations
│   │   └── src/main/resources/db/migration/V1__initial_schema.sql
│   └── paper-erp-api/                  # REST Controllers, Security, Actuator, Health check
│       └── src/test/java/com/papererp/api/  # FlywayMigrationTest & PaperErpApplicationTests
├── frontend/
│   ├── src/                            # React 19 application components
│   │   ├── App.tsx                     # Arabic RTL operations dashboard & health smoke test
│   │   └── App.test.tsx                # Automated component test
│   ├── index.html                      # Root HTML with dir="rtl" and Cairo font
│   ├── vite.config.ts                  # Vite config with API proxy to port 8080
│   └── package.json
├── docker-compose.yml                  # PostgreSQL 16 development database service
├── .env.example                        # Template for environment variables
└── README.md
```

---

## 3. Quickstart & Local Development

### 3.1 Start the Development Database
You can run PostgreSQL 16 using Docker Compose, or point the application to an existing PostgreSQL instance:

```bash
# Start PostgreSQL container in background
docker compose up -d

# Check container health status
docker compose ps
```

*Default credentials:*  
* Database: `papererp`
* Username: `papererp`
* Password: `papererp_dev_password`
* Port: `5432`

---

### 3.2 Backend Setup & Execution
The backend uses the bundled **Maven Wrapper**, so you do not need Maven installed globally.

```powershell
# Navigate to backend directory
cd backend

# Execute automated tests and Flyway schema verification
.\mvnw.cmd clean test       # On Windows PowerShell
./mvnw clean test           # On Linux / macOS / Bash

# Start the Spring Boot API server on port 8080
.\mvnw.cmd spring-boot:run
```

Once started, verify the backend health endpoint:
* Health Smoke Test: [http://localhost:8080/api/v1/health](http://localhost:8080/api/v1/health)
* Actuator Health: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

---

### 3.3 Frontend Setup & Execution
The frontend runs on Vite with automatic proxying to the backend API (`/api` $\rightarrow$ `http://localhost:8080`).

```powershell
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Run automated tests
npm test

# Start the Vite development server on port 5173
npm run dev

# Verify production bundle build
npm run build
```

Open your browser at [http://localhost:5173](http://localhost:5173).

---

## 4. Windows PowerShell Troubleshooting

### 4.1 npm Script Execution Policy
If you encounter `File npm.ps1 cannot be loaded because running scripts is disabled on this system`, run one of the following:

```powershell
# Option 1 (Recommended): Set RemoteSigned policy for Current User
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned

# Option 2: Run npm directly via cmd
cmd /c npm test
cmd /c npm run dev
```

### 4.2 Maven Wrapper Execution
Always invoke the wrapper from the `backend/` directory:
* Windows: `.\mvnw.cmd <command>`
* Linux / macOS: `./mvnw <command>`

---

## 5. Team Code Ownership & Git Workflow

This project is maintained by a 3-person remote engineering team. Code ownership is divided to prevent overlapping changes:

* **Developer A (Architecture, Security, Platform & DevOps)**:
  * Owns: `backend/paper-erp-common/`, `backend/paper-erp-api/src/main/java/com/papererp/api/config/`, `.github/workflows/`, `docker-compose.yml`.
  * Focus: Spring Security, JWT, Multi-Tenancy resolution (`TenantContext`), CI/CD pipeline.
* **Developer B (Manufacturing Domain, Database Schema & Ledger)**:
  * Owns: `backend/paper-erp-core/`, `backend/paper-erp-core/src/main/resources/db/migration/`.
  * Focus: PostgreSQL Flyway migrations, immutable inventory ledger, slitting execution, QC gates.
* **Developer C (Frontend Lead, Arabic RTL Experience & Testing)**:
  * Owns: `frontend/`.
  * Focus: React 19 SPA, Arabic RTL layouts, Cairo typography, slitting cut calculator screen.

### Git Branch & PR Workflow:
1. All work is done on short-lived feature branches: `feat/issue-<id>-<description>`.
2. Direct pushes to `main` are strictly blocked.
3. Every Pull Request must pass the automated GitHub Actions CI checks (`backend-ci`, `frontend-ci`) and receive at least 1 approving review before merging.
