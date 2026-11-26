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
4. **Published**: `dev.shanelucy:db-documenter` (library, Java 21) and `dev.shanelucy:db-documenter-cli` (fat jar classifier) on Maven
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

### Decisions from the plan review (user, 2026-11-26)

`tech-lead-reviewer` reviewed this plan before Phase 0; the user accepted these changes:

| Topic | Decision |
|---|---|
| Artifacts | `dev.shanelucy:db-documenter` (library) + `dev.shanelucy:db-documenter-cli` (thin jar + fat-jar classifier). The parent POM is published too (or flattened). Supersedes "CLI fat jar as classifier of the library". |
| Public catalog model | **Public records** in an exported `catalog` package, built via builders. Adding a component breaks record patterns and the canonical constructor, so **1.0 is not released until the model is believed stable** (all four vendor families read into it, and the deferred facts below are considered). Accepted risk. |
| Reader selection | `MetadataReader` stays **internal**; an internal static registry selects it by `getDatabaseProductName()`, plus the version for MariaDB. No ServiceLoader and no public SPI at 1.0. |
| Reader implementation | Native **bulk** catalog queries per vendor (`pg_catalog`, MySQL `information_schema`, SQL Server `sys.*`). A `DatabaseMetaData` reader is kept only as a fallback and test oracle. SQLite is exempt from the scale target (its pragmas work one table at a time). |
| Derived facts | Cardinality, 1:1 and composite nullability are computed by pure functions (renderer or model helpers), never by readers. |
| Golden strategy | Phase 1 adds a canonical sorted **catalog dump** golden (one fact per line). Reader ITs assert against it, and it becomes the cross-vendor expected output in Phase 4. Renderer tests use hand-built snapshots. PUML goldens are regenerated **once**, in Phase 2, and reviewed by `puml-schema-validator`. |
| Phase 1 spike | Read a second vendor (SQLite or MySQL) into `CatalogSnapshot` as a throwaway spike, so the model isn't shaped by `pg_catalog` alone. |

**Catalog model sketch:** all immutable, deterministic order (schemas and tables by name, columns by ordinal, key columns by key position).
- `CatalogSnapshot { productName, productVersion, List<Schema> }`
- `QualifiedName { schema, name }`. The `Dialect` maps vendor namespaces onto it: MySQL database → schema, SQL Server schema, SQLite `main`/attached databases.
- `Table { QualifiedName, List<Column>, Optional<Key> primaryKey, List<Key> uniqueKeys (constraint vs unique index), List<ForeignKey> }`
- `Column { name, ordinal, nullable, DataType { displayText, Optional<QualifiedName> userType } }`
- `ForeignKey { name, ordered column pairs, referenced QualifiedName, onDelete, onUpdate }`
- `EnumType { QualifiedName, ordered values }`. Decide in Phase 1 how MySQL inline `ENUM(...)` columns are represented.
- Facts deliberately considered before freezing the model at 1.0: check constraints, comments, views, non-unique indexes.

**Public at 1.0:**
- `DbDocumenter`, the config class and builder, `DbDocumenterException`, `DiagramResult`, the catalog model, `readCatalog()`.
- Documented connection ownership: a caller-supplied `Connection` stays caller-owned; with a `DataSource`, the library opens and closes one connection per run.

**Internal at 1.0:** readers, dialects, renderer. `DatabaseType.getUrlTemplate()` is deleted.

**Risks flagged for later phases:**
- **Driver licensing (Phase 4/5).** MySQL Connector/J is GPLv2 with the FOSS Exception; MariaDB Connector/J is LGPL-2.1. Bundling either in the Apache-2.0 fat jar and image needs a deliberate choice and NOTICE entries.
- **SQLite in the Alpine image.** sqlite-jdbc ships musl/aarch64 natives and extracts them to tmp; check read-only rootfs.
- **Identifier case folding** differs per vendor. Define it per `Dialect`, which affects H3 schema validation.
- **Least privilege per vendor.** SQL Server needs `VIEW DEFINITION`. Decide whether "no constraints visible" warns or exits non-zero.
- **Scale test.** The 2,000-table test asserts the **query count** via a counting `Connection`/`DataSource` proxy, not wall-clock time, and is tagged so it doesn't dominate every `verify`.
- **Portable fixture.** A common core plus per-vendor extensions: SQLite has no schemas (needs `ATTACH`) and needs `PRAGMA foreign_keys`, and MySQL has no named enums.
- **Java 21 runtime.** `--release 21` checks the API only. Add a CI job that runs the core tests on a JDK 21 JVM (Phase 0 or 7).

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
| 0 | Package/module rename; multi-module split (`core` library on Java 21, `cli` on Java 25); drivers non-transitive for the library (steps below) | DONE |
| 1 | New core: vendor-neutral `CatalogSnapshot` model + `MetadataReader` SPI; PostgreSQL reader on `pg_catalog` with bulk per-schema queries; public record catalog model; catalog-dump golden; second-vendor spike; least-privilege + query-count ITs; corrected fixtures | NOT STARTED |
| 2 | Rendering: schema-qualified aliases + escaping, correct cardinality + 1:1, UNIQUE semantics (composite/unique indexes), deterministic name ordering, types via `format_type` | NOT STARTED |
| 3 | Public API + CLI: DataSource/JDBC URL, config builder class, exported exception, `readCatalog()`, password env/file + optional, SSL options/driver properties, schema validation, quiet logging + `--verbose`, UTF-8 stdout | NOT STARTED |
| 4 | Vendors: native bulk-query readers MySQL/MariaDB → SQL Server → SQLite (`DatabaseMetaData` reader as fallback/oracle); shared portable fixture (common core + per-vendor extensions), parameterised Testcontainers ITs; internal static registry selection | NOT STARTED |
| 5 | Maven Central: `dev.shanelucy`, POM metadata (developers), sources jar, GPG signing, `central-publishing-maven-plugin`, CLI fat jar classifier, licence/NOTICE bundling, Multi-Release manifest | NOT STARTED |
| 6 | Javadoc on GitHub Pages: docs profile, versioned public + internal sites, Pages deploy on release | NOT STARTED |
| 7 | Hardening: Dependabot (maven, docker, actions), SECURITY.md, CHANGELOG, Sonar token step-scoped + plugin pinned, SBOM/provenance/signing, multi-arch image, digest-pinned base, PG version matrix, Maven wrapper + reproducible builds, release ordering | NOT STARTED |
| 8 | Final independent review → fixes → set `1.0.0` → user tags | NOT STARTED |

### Phase 0 steps

Phase 0 is purely mechanical. Each step ends with a green `mvn -B clean verify` and its own commit.

| Step | Work | Status |
|---|---|---|
| 0.1 | Rename `db.documenter` → `dev.shanelucy.dbdocumenter` (packages, module, `git mv`), groupId → `dev.shanelucy`; update pom references (mainClass, `--add-reads`, javadoc package lists), spotbugs-exclude regex, `.claude/skills/*/SKILL.md`, CLAUDE.md, README. No other changes. | DONE |
| 0.2 | Scope hygiene: `spotbugs-annotations` → `provided` + `requires static`; drop the main-compile `--add-reads …=ALL-UNNAMED` if no longer needed; a `build.jdk` property for the enforcer; commit `.mvn/` (Maven wrapper) | DONE |
| 0.3 | Parent POM (`db-documenter-parent`, `pom` packaging, plugin/dependency management) + one child `db-documenter/` with everything moved unchanged. Config file paths via `${maven.multiModuleProjectDirectory}`. Dockerfile, `.dockerignore`, `ApplicationDockerIT` (Dockerfile path via system property) and `release.yml` paths updated, keeping CRLF. Per-verify javadoc may move to a `docs` profile here. | DONE: javadoc runs once per verify (public API, doclint). The internal (`show=private`) site moved to `-Pdocs`, so doclint on private members now runs only there; Phase 6 CI should run `-Pdocs`. Sites nest as `site/{apidocs,internal-apidocs}/apidocs/` (relevant for Pages). |
| 0.4 | `db-documenter-test-support` (not deployed, no module-info): `testhelpers/*`, SQL fixtures, goldens read as classpath resources | DONE |
| 0.5 | Split out `db-documenter-cli`. Its module-info: `@NullMarked`, `requires dev.shanelucy.dbdocumenter`, picocli, `requires static org.jspecify`. Core drops picocli and gets **temporary** qualified exports of `internal.{exceptions,utils,validation}` to the cli module (TODO: remove in Phase 3). Shade/manifest/Dockerfile wiring moves to cli; core jar manifest loses `mainClass`/`Class-Path`. Per-module JaCoCo thresholds set after measuring. | DONE: unit coverage is library 97.3% instructions / 81.9% branches, cli 86.0% / 86.1%, so the shared 0.80 thresholds are kept. The library's branch margin is thin. FindSecBugs can't see the library's `LogUtils.sanitizeForLog` from the cli module, so `findsecbugs-taint.txt` declares it CR/LF-safe (no suppression). **Update that file if the method's signature changes in Phase 3.** |
| 0.6 | Core `--release 21` (modernizer and javadoc 21); cli `--release 25`. Confirm NullAway still fires in both modules with a deliberate violation, then revert. | DONE: library and test-support produce class version 65, cli 69. NullAway was proven active in both modules. Phase 7 JDK 21 runtime job: a second `setup-java` 21 step in `build.yaml` running `./mvnw -B surefire:test -pl db-documenter -Djvm=$JAVA_HOME_21_X64/bin/java` (~12 lines). javac doesn't reject dependencies compiled for a newer Java version, so only that job proves the library runs on 21. A `MainTest$IntegrationTests` Testcontainers flake (connection refused under WSL) was seen once. |
| 0.7 | Drivers non-transitive: core `postgresql` `runtime` + `optional`; cli declares it explicitly. Check `dependency:tree` and `jar tf` on the fat jar; `ApplicationDockerIT` must pass. | DONE: a test consumer pom gets no transitive dependencies from the library. The CLI shade config gains `ServicesResourceTransformer`, which merges `META-INF/services` files so a second JDBC driver in Phase 4 doesn't overwrite the first. |
| 0.8 | Housekeeping: untrack `.idea/*`, rename the Docker-requiring `*Test` classes to `*IT`, update CLAUDE.md "Key Project Characteristics", update the status here | DONE: Docker-dependent tests moved to failsafe: `DbDocumenterIT`, `PostgresqlQueryRunnerIT`, and `MainIT` (the 3 Docker nested classes of `MainTest`). Counts: 355 surefire + 98 failsafe = 453. `mvn test` was proven to run without Docker. JaCoCo merges unit and integration data (coverage unchanged). `.idea/` is untracked. |

**Phase 0 must NOT:**
- change behaviour or golden output, or fix C1/C2/H1–H3
- introduce `CatalogSnapshot`/`MetadataReader`/`Dialect` or the public exception
- restructure the `internal.*` layers that Phase 1 replaces
- change the public API
- add drivers, publishing or Pages work

After the first push of the split, check that SonarCloud picks up both modules: the new-code baseline resets on renamed paths.

## User actions required (cannot be done by Claude)

These have lead time: start them now, in parallel with Phases 0–4.

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
