# PaperERP Enterprise — Staging Deployment Runbook

**Document ID:** `OPS-STAGING-DEPLOY-001`  
**Target Environment:** Staging Acceptance Cluster (Isolated Kubernetes / VM Infrastructure)  
**Governing Roles:** Senior DevOps Engineer, Release Manager, Security Auditor, ERP QA Lead  
**Artifact Baseline:** `paper-erp-api-1.0.0-SNAPSHOT.jar` (SHA-256: `4B543B1170F6AF1D6CC8BDE97E8A32711B14817ADEB4097336FACA70123E7821`)  
**Commit Baseline:** `92dbc29d81976bff7a791c902bbef7effc394552` (Branch: `master`)  

---

## 1. Safety Governance & Operator Rules

> [!IMPORTANT]
> **Operational Execution Rules:**
> 1. **No Production Access:** Staging deployment operations must never connect to, point to, or touch production databases or credentials (`db.<prod>.supabase.co`).
> 2. **Secret Redaction:** Never output, log, or commit secret values (passwords, JWT secrets, database connection strings containing credentials).
> 3. **Owner Authorization Labels:** Every command in this runbook is labeled as either:
>    - `[READ-ONLY / SAFE]`: Diagnostic probe; safe to execute anytime.
>    - `[REQUIRES OWNER AUTHORIZATION]`: State-changing deployment, migration, or infrastructure modification.

---

## 2. Phase 1: Pre-Deployment Prerequisite Checks

Before initiating any deployment command, the DevOps engineer must verify the availability and reachability of all staging infrastructure components.

### 2.1 Database Connectivity & Privilege Probe `[READ-ONLY / SAFE]`
Execute from an authorized management host using the restricted staging role:
```bash
psql -U paper_erp_app_role -h $STAGING_DB_HOST -p 5432 -d $STAGING_DB_NAME -c "
SELECT current_user, session_user, pg_is_in_recovery();
SELECT tablename, rowsecurity FROM pg_tables WHERE schemaname = 'public' LIMIT 5;
"
```
- **Pass Criteria:** `current_user` is `paper_erp_app_role` (non-superuser); `rowsecurity` is `true`.

### 2.2 Redis Connectivity & TLS Probe `[READ-ONLY / SAFE]`
```bash
redis-cli -h $STAGING_REDIS_HOST -p 6379 -a "$STAGING_REDIS_PASSWORD" --tls ping
```
- **Pass Criteria:** Returns `PONG`.

### 2.3 Edge Reverse Proxy Header Sanitization Check `[READ-ONLY / SAFE]`
Verify that the staging ingress controller (e.g. AWS ALB, Ingress NGINX, Cloudflare) is configured to overwrite client-supplied `X-Forwarded-For`:
```bash
# Send a test request with a forged X-Forwarded-For header
curl -k -H "X-Forwarded-For: 203.0.113.195" https://$STAGING_INGRESS_HOST/api/v1/health
```
- Inspect ingress access logs: The logged `$http_x_forwarded_for` or client IP must reflect the actual test runner IP, **not** the spoofed `203.0.113.195`.

---

## 3. Phase 2: Container Packaging & Deployment Execution

### 3.1 Container Image Build `[REQUIRES OWNER AUTHORIZATION]`
Execute from repository root `G:\ERP_BackEnd`:
```bash
# 1. Confirm JAR artifact exists and matches verified hash
sha256sum backend/paper-erp-api/target/paper-erp-api-1.0.0-SNAPSHOT.jar
# Expected: 4B543B1170F6AF1D6CC8BDE97E8A32711B14817ADEB4097336FACA70123E7821

# 2. Build non-root container image
docker build -t papererp-backend:1.0.0-staging -f backend/Dockerfile backend/
```

### 3.2 Kubernetes Deployment Manifest `[REQUIRES OWNER AUTHORIZATION]`
Deploy the container with 2 replicas to validate distributed multi-instance coordination:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: papererp-backend-staging
  namespace: papererp-staging
  labels:
    app: papererp-backend
    env: staging
spec:
  replicas: 2
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: papererp-backend
  template:
    metadata:
      labels:
        app: papererp-backend
        env: staging
    spec:
      securityContext:
        runAsNonRoot: true
        runAsUser: 10001
        runAsGroup: 10001
        fsGroup: 10001
      containers:
      - name: api
        image: papererp-backend:1.0.0-staging
        imagePullPolicy: IfNotPresent
        ports:
        - containerPort: 8080
          name: http
        resources:
          requests:
            memory: "1024Mi"
            cpu: "500m"
          limits:
            memory: "2048Mi"
            cpu: "2000m"
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SERVER_FORWARD_HEADERS_STRATEGY
          value: "framework"
        - name: RATE_LIMITING_REDIS_FAILURE_MODE
          value: "fail-closed"
        - name: SUPABASE_DB_URL
          valueFrom:
            secretKeyRef:
              name: papererp-staging-secrets
              key: database-url
        - name: SUPABASE_DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: papererp-staging-secrets
              key: database-username
        - name: SUPABASE_DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: papererp-staging-secrets
              key: database-password
        - name: REDIS_HOST
          valueFrom:
            secretKeyRef:
              name: papererp-staging-secrets
              key: redis-host
        - name: REDIS_PORT
          value: "6379"
        - name: REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: papererp-staging-secrets
              key: redis-password
        - name: REDIS_SSL_ENABLED
          value: "true"
        - name: APP_JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: papererp-staging-secrets
              key: jwt-secret
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 45
          periodSeconds: 15
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 2
```

---

## 4. Phase 3: Post-Deployment Smoke Verification `[READ-ONLY / SAFE]`

Execute the following verification suite immediately after pod deployment:

### 4.1 Pod Startup & Health Probes
```bash
# 1. Verify pods are running and ready
kubectl get pods -n papererp-staging -l app=papererp-backend

# 2. Inspect logs for successful Flyway migration and Redis startup
kubectl logs -n papererp-staging -l app=papererp-backend --tail=100 | grep -E "Flyway|Redis rate-limiting|Started PaperErpApplication"
```
- **Expected Log Signatures:**
  - `Successfully validated 3 migrations`
  - `Current version of schema "public": 3`
  - `Redis rate-limiting backend verified at startup. Ping: PONG`
  - `Started PaperErpApplication in ... seconds`

### 4.2 Endpoint Health Ingress Probe
```bash
curl -i https://$STAGING_INGRESS_HOST/api/v1/health
```
- **Expected Response:** HTTP 200 OK `{"success":true,"message":"System is healthy and operational",...}`.

```bash
curl -i https://$STAGING_INGRESS_HOST/actuator/health
```
- **Expected Response:** HTTP 200 OK `{"status":"UP"}`.

### 4.3 Distributed Rate-Limiting Multi-Instance Verification
Verify that both pod replicas share the same Redis rate-limiting counter:
```bash
# Send 5 invalid login attempts to trigger account lockout
for i in {1..5}; do
  curl -s -X POST https://$STAGING_INGRESS_HOST/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"uat_test_operator","password":"WrongPassword123!"}' | jq .
done

# The 6th request must return HTTP 429 across whichever pod handles it
curl -i -X POST https://$STAGING_INGRESS_HOST/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"uat_test_operator","password":"WrongPassword123!"}'
```
- **Expected Response:** HTTP 429 `ACCOUNT_LOCKED_OUT`.

---

## 5. Phase 4: Rollback Protocol

### 5.1 Application-First Rollback `[REQUIRES OWNER AUTHORIZATION]`
If readiness probes fail or severe regressions are discovered during smoke tests:
```bash
# Instantly roll back to previous stable container revision
kubectl rollout undo deployment/papererp-backend-staging -n papererp-staging

# Monitor rollback status
kubectl rollout status deployment/papererp-backend-staging -n papererp-staging
```
- **Data Safety Assurance:** Because all Flyway migrations (V1, V2, V3) follow the **Expand/Contract Pattern** with strictly additive columns and backward-compatible indexes, the previous application version runs without errors against the V3 schema. Zero data is dropped.

### 5.2 Pre-Traffic Disaster Restoration (Pre-Traffic Only) `[REQUIRES OWNER AUTHORIZATION]`
If database corruption occurs *before* any business traffic is allowed onto staging:
```bash
pg_restore -U postgres -h $STAGING_DB_HOST -p 5432 --clean --if-exists --no-owner --no-privileges \
  -d $STAGING_DB_NAME /secure/backups/papererp_staging_pre_deployment.dump
```
