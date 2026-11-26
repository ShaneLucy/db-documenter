# db-documenter — Road to 1.0.0

Created 2026-11-26. Supersedes the "Road to 1.0.0" section of `PROJECT_COMPLETION_PLAN.md` (which
remains the record of the 0.x work). Source: three independent deep reviews (correctness, architecture
& API, engineering & delivery) run on HEAD `4751a2a`, deliberately **not** judged against the project's
own standards; findings summarised in the appendix.

## Outcome (definition of done for 1.0.0)

A correct, fast, multi-vendor ERD generator, published as a library and CLI:

1. **Correct output** for PostgreSQL, MySQL, MariaDB, SQL Server and SQLite — composite keys, cross-schema
   references, least-privilege (SELECT-only) users, one-to-one relationships, crow's-foot cardinality,
   arbitrary identifiers — proven by a shared vendor-neutral fixture run against every vendor.
2. **Scales**: a bounded number of metadata queries per schema (no per-table N+1); a 2,000-table test.
3. **Stable public API** (semver from 1.0): `DataSource`/JDBC-URL connection, builder-based config, a read-only
   catalog model, a public exception type, documented thread-safety and connection ownership.
4. **Published**: `dev.shanelucy:db-documenter` (library, Java 21) and the CLI fat jar (classifier) on Maven
   Central, signed, with sources + Javadoc; Docker image (multi-arch) on GHCR; GitHub Release.
5. **Docs**: versioned public + internal Javadoc on GitHub Pages; README accurate for all vendors.
6. **Delivery hygiene**: licence notices in shipped artifacts, Dependabot, SECURITY.md, CHANGELOG, SBOM/
   provenance, Sonar quality gate green.

## Decisions (user, 2026-11-26)

| Topic | Decision |
|---|---|
| Vendors for 1.0 | PostgreSQL, MySQL + MariaDB, SQL Server, SQLite (**not** Oracle) |
| Maven groupId | `dev.shanelucy` (domain shanelucy.dev; DNS TXT verification by user) |
| Java package / JPMS module | rename `db.documenter` → `dev.shanelucy.dbdocumenter` before 1.0 |
| Java baseline | library `--release 21`; CLI + Docker image on Java 25 |
| Maven Central artifacts | library + CLI fat jar (classifier) |
| Javadoc on GitHub Pages | public **and** internal, versioned per release + `latest` alias (internal site carries a "not public API" banner) |
| Versioning | development on `0.x-SNAPSHOT`; first release is `1.0.0` |

## Starting a fresh session (read this first)

This plan is designed to be picked up by a new Claude Code session with no prior conversation.

1. Read this file end to end, then `CLAUDE.md` (agent routing, Maven permissions) and skim
   `PROJECT_COMPLETION_PLAN.md` (history of the 0.x work, known patterns and pitfalls).
2. `git log --oneline -10` — the 0.x work ends at `4751a2a` (plan committed on top of it).
3. **Before any code**: have `tech-lead-reviewer` review this plan (phase order, the Phase 0/1 boundary, the
   `MetadataReader`/`CatalogSnapshot` design, multi-module layout with Java 21 core + Java 25 CLI under JPMS +
   NullAway). Fold its findings into this file, then start Phase 0.
4. Work one phase at a time; update the Phases table status as you go so progress survives context compaction.

**Environment facts (WSL2, verified 2026-11-26):**
- JDK 25.0.1 (Temurin via sdkman), Maven 3.8.7, Docker Desktop with WSL integration (Testcontainers works).
- `gh` CLI is **not** installed; `jq` is not installed (use `python3` for JSON).
- SonarCloud project `ShaneLucy_db-documenter`, org `shanelucy`; public API readable without auth, e.g.
  `curl -s "https://sonarcloud.io/api/qualitygates/project_status?projectKey=ShaneLucy_db-documenter"`.
- Workflow files use **CRLF** line endings — preserve them (edit via a script that keeps `\r\n`).

**Pitfalls learned in the 0.x work (see `PROJECT_COMPLETION_PLAN.md` for detail):**
- Never edit files while a `verify` run is in progress — it invalidates the run.
- A background build agent's "finished" notification can arrive before Maven exits; confirm from the log.
- SpotBugs can't see that `Validators.*` returns non-null — don't chain calls on the result.
- Builders store lists without copying; `EI_EXPOSE_REP2` is excluded for `*$Builder` only.
- Error Prone + NullAway run on main code only, with `-J--add-exports` flags because the compiler is forked.
- Golden files can encode bugs — review regenerated expected output, don't just accept it.

## Working agreement (unchanged from 0.x work)

- Commit to `main` after each green phase; **never push or tag without the user's explicit instruction**.
- Gate before every commit: `mvn -B clean verify` (unit, Docker ITs, Error Prone/NullAway, SpotBugs, Checkstyle,
  PMD/CPD, modernizer, JaCoCo) via `maven-architect`; tree frozen while it runs.
- Agent routing per `CLAUDE.md`; `tech-lead-reviewer` reviews each phase plan before coding.
- Golden/expected outputs are regenerated deliberately and **reviewed for correctness** (the old ones encoded bugs).

## Phases

| # | Phase | Status |
|---|---|---|
| 0 | Package/module rename; multi-module split (`core` library on Java 21, `cli` on Java 25); drivers non-transitive for the library | NOT STARTED |
| 1 | New core: vendor-neutral `CatalogSnapshot` model + `MetadataReader` SPI; PostgreSQL reader on `pg_catalog` with bulk per-schema queries; least-privilege + performance ITs; corrected fixtures | NOT STARTED |
| 2 | Rendering: schema-qualified aliases + escaping, correct cardinality + 1:1, UNIQUE semantics (composite/unique indexes), deterministic name ordering, types via `format_type` | NOT STARTED |
| 3 | Public API + CLI: DataSource/JDBC URL, config builder class, exported exception, `readCatalog()`, password env/file + optional, SSL options/driver properties, schema validation, quiet logging + `--verbose`, UTF-8 stdout | NOT STARTED |
| 4 | Vendors: `DatabaseMetaData`-based base reader; MySQL/MariaDB → SQL Server → SQLite; shared portable fixture, parameterised Testcontainers ITs; ServiceLoader selection | NOT STARTED |
| 5 | Maven Central: `dev.shanelucy`, POM metadata (developers), sources jar, GPG signing, `central-publishing-maven-plugin`, CLI fat jar classifier, licence/NOTICE bundling, Multi-Release manifest | NOT STARTED |
| 6 | Javadoc on GitHub Pages: docs profile, versioned public + internal sites, Pages deploy on release | NOT STARTED |
| 7 | Hardening: Dependabot (maven, docker, actions), SECURITY.md, CHANGELOG, Sonar token step-scoped + plugin pinned, SBOM/provenance/signing, multi-arch image, digest-pinned base, PG version matrix, Maven wrapper + reproducible builds, release ordering | NOT STARTED |
| 8 | Final independent review → fixes → set `1.0.0` → user tags | NOT STARTED |

## User actions required (cannot be done by Claude)

- Add the DNS TXT record for `shanelucy.dev` in the Central Portal; create a Central user token.
- Generate a GPG key, publish it to keyservers.
- Add GitHub secrets: Central token username/password, GPG private key + passphrase.
- Enable GitHub Pages (source: GitHub Actions).

## Appendix — deep review findings (2026-11-26, HEAD 4751a2a)

### Correctness (verified by running the jar against PostgreSQL 16)
- **C1 (critical)** FK query joins `constraint_column_usage` on constraint name only → composite FKs multiply
  (up to 16×), auto-named FKs (`orders_uid_fkey`) in sibling schemas produce bogus cross-schema edges, FKs to
  partitioned tables emit edges to child partitions. Golden file already encodes it (4× `warehouse_locations
  ||--|{ inventory_bins`). Fix: `pg_constraint` (`conparentid = 0`, `unnest(conkey, confkey) WITH ORDINALITY`).
- **C2 (critical)** SELECT-only users get no PK/FK/UNIQUE/edges, exit 0 (`information_schema` privilege filtering).
- **H1** Relationship lines use raw unqualified names: spaces/hyphens break rendering; same-named tables in two
  schemas attach edges to phantom boxes. Fix: schema-qualified sanitised aliases, escape `"`.
- **H2** Cardinality inverted: NOT NULL FK should be `||--o{`, nullable `|o--o{`; 1:1 (FK that is PK/UNIQUE)
  should be `||--o|`; composite nullability from one column only. README row documents the wrong mapping.
- **H3** Non-existent / mis-cased / whitespace schema names → empty packages, exit 0.
- **M** Array types lose info (`int4[]`, lengths dropped, cross-schema enum arrays unqualified) → `format_type`;
  composite UNIQUE members marked `<<UNIQUE>>`, unique indexes missed; stdout encoding follows locale;
  JDBC URL built by `String.format` (`--database='x?sslmode=…'` injects params); no sslmode/rootcert; blank
  password rejected; ~118 INFO log lines on stderr per run.
- **L** PK order lacks `ORDER BY ordinal_position`; tables ordered by OID; same-schema composite types qualified
  but enums not; one connection per schema; `Unexpected error: null`; stdout write errors swallowed.
- Verified correct: self-refs, FK to unique non-PK column, views/partitions excluded, numeric/char lengths,
  enum value order, byte-identical repeat runs, exit codes 1/2, `_enums` naming.

### Architecture & API
- N+1: 3 queries per table + 1 per enum; `ColumnMappingContext` copies schema-wide maps per table (quadratic).
- "Generic" layer is PostgreSQL-only (sealed hierarchies permit only PG; `SqlAgnostic` SQL overridden → dead;
  builders take `PostgresqlQueryRunner`; `DbDocumenter` ignores `databaseType`; `"USER-DEFINED"` assumptions).
- Too many layers per query (SQL holder + binder + mapper + runner); formatter chains patch strings.
- Library leaks CLI deps (picocli compile, `requires info.picocli`), `spotbugs-annotations` compile scope,
  driver transitive. `DatabaseType.getUrlTemplate()` public but unused.
- Target: `MetadataReader` SPI (per vendor) → `CatalogSnapshot`; `JdbcMetadataReader` on `DatabaseMetaData` as
  base with vendor overrides; `Dialect` (quoting, type display, namespace mapping); pure renderer;
  ServiceLoader by `getDatabaseProductName()`; shared portable DDL fixture across vendors.
- API before 1.0: DataSource/connection supplier + JDBC URL; config as final class + builder (not record);
  `readCatalog()` separate from rendering; exported `DbDocumenterException`; japicmp/revapi after 1.0.

### Engineering & delivery
- Password only via argv (visible in `ps`, `docker inspect`, CI logs); `ssl=true` ⇒ verify-full needs
  `~/.postgresql/root.crt` which the image can't have.
- Fat jar: pgjdbc's LICENSE overwrites others, no NOTICE/third-party list; no `Multi-Release: true`;
  bogus `Class-Path`.
- No SBOM/provenance/signing; base image not digest-pinned; amd64 only; no Dependabot.
- `SONAR_TOKEN` visible to whole `mvn verify`; Sonar plugin unversioned; CI runs twice for same-repo PRs.
- Image pushed before GitHub Release (half-published risk); release procedure undocumented.
- Tests: single PG version (15.3); no SSL / restricted-role ITs; some interaction-only mock tests;
  `*Test` classes need Docker; surefire `parallel` config is inert; first `ApplicationDockerIT` test
  doesn't assert exit code.
- Javadoc generated 3× per verify (move to profile); no Maven wrapper / `outputTimestamp`; image ~308 MB.
- Missing: CHANGELOG, CONTRIBUTING, SECURITY.md, templates, dependabot.yml; `.idea/*` tracked; README lacks
  supported versions/required privileges; README Java example uses generic `PostgreSQLContainer<>`.
