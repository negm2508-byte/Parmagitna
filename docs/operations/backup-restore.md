# PaperERP Enterprise — Database Backup and Disaster Recovery Procedure

## 1. Backup Strategy

PaperERP Enterprise stores critical manufacturing genealogy, legal inventory movements, and customer compliance data. Backups must be performed regularly and tested periodically.

### Automated Backups
Supabase provides automated daily WAL-based backups. For enterprise compliance, daily or pre-migration logical dumps (`pg_dump`) must also be maintained.

## 2. Pre-Migration Snapshot Procedure

BEFORE applying any forward-only migration or schema modification to a production Supabase instance:

```powershell
# Export complete schema and data snapshot
pg_dump `
  --host="db.<project-ref>.supabase.co" `
  --port=5432 `
  --username="postgres" `
  --format=custom `
  --file="paper_erp_backup_$(Get-Date -Format 'yyyyMMdd_HHmmss').dump" `
  postgres
```

## 3. Disaster Recovery & Restoration

To restore from a snapshot into a staging or recovery database:

```powershell
# Restore into target recovery database
pg_restore `
  --host="db.<recovery-ref>.supabase.co" `
  --port=5432 `
  --username="postgres" `
  --clean `
  --if-exists `
  --no-owner `
  --no-privileges `
  --dbname="postgres" `
  "paper_erp_backup_20261026_100000.dump"
```

## 4. Verification After Recovery

1. Check table row counts:
   ```sql
   SELECT count(*) FROM jumbo_rolls;
   SELECT count(*) FROM slit_rolls;
   SELECT count(*) FROM inventory_ledger;
   ```
2. Verify Flyway schema history integrity:
   ```sql
   SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;
   ```
3. Run the backend read-only smoke tests against the restored database.
