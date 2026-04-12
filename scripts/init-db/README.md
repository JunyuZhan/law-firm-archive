# Database Init Scripts

## Files

- `02-schema-consolidated.sql`: recommended initialization script for new PostgreSQL environments.
- `01-schema.sql`: historical initialization script kept for reference only.

## Recommended Usage

- New PostgreSQL environment: execute `02-schema-consolidated.sql`.
- Docker Compose deployment: mount only `02-schema-consolidated.sql` into `/docker-entrypoint-initdb.d` to avoid executing historical scripts together with the recommended schema.
- Existing environment already initialized from older scripts: do not rerun schema scripts directly against live data; review differences first and prepare an explicit migration plan.
- H2 test environment: use `backend/src/test/resources/schema-current.sql` through `application-test.yml`, not the PostgreSQL scripts in this directory.

## Important Notes

- The application supports uploading digital files before they are bound to an archive, so `arc_digital_file.archive_id` must remain nullable during initialization.
- Default roles and `sys_user_role` seed data are included in `02-schema-consolidated.sql`.
- This directory targets PostgreSQL initialization. Test-only H2 schema files are maintained separately under `backend/src/test/resources/`.
