## Description of Changes
<!-- Provide a concise summary of the changes introduced by this PR -->

- **Related Issue**: Fixes #
- **Sprint**: Sprint 1 Foundation

## Architecture & Code Quality Checklist
- [ ] Multi-tenancy: No queries bypass tenant isolation (`tenant_id` enforced)
- [ ] Database: Flyway migrations are backward-compatible and tested on a clean DB
- [ ] Security: Server-side validation and `@PreAuthorize` permissions verified
- [ ] RTL: UI components tested in Arabic RTL mode (`dir="rtl"`) with Cairo font
- [ ] Configuration: No hardcoded secrets, paths, or production credentials

## Testing & Verification Evidence
- [ ] Backend tests passing: `./mvnw clean test`
- [ ] Frontend tests passing: `npm test`
- [ ] Clean build passing: `./mvnw clean verify` & `npm run build`
<!-- Attach test execution output or screenshot here -->
