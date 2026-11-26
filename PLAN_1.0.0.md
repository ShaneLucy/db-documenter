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
2. `git log --oneline -15`. The 0.x work ends at `4751a2a`. **Phase 0 is complete and pushed** (ends at
   `874989c`; SonarCloud gate green after the split). The whole-plan review has been done and folded in: see
   "Decisions from the plan review" and the "Phase 0 completion review" carry-forward items.
3. **Phase 1 is in progress.** The step table under "Phase 1 steps" has been reviewed by `tech-lead-reviewer`,
   the findings and user decisions are folded in, and each step's status column records progress. Resume at the
   first step that is not DONE.
4. Work one phase at a time; update the Phases table status as you go so progress survives context compaction.

**Environment facts (WSL2, verified 2026-11-26):**
- JDK 25.0.1 (Temurin via sdkman); use the Maven wrapper `./mvnw` (3.9.16; system Maven is 3.8.7); Docker Desktop with WSL integration (Testcontainers works).
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

**Pitfalls learned in Phase 0:**
- **IntelliJ rewrites `.idea/` during builds.** It is now untracked.
- **Known flake:** Testcontainers "connection refused" under WSL. Re-run once before investigating.
- **Auto-mode blocks security suppressions** (SpotBugs excludes, `@SuppressFBWarnings`). Ask the user. Prefer fixes that suppress nothing, such as `findsecbugs-taint.txt`.
- **Dependency convergence is reactor-wide.** A compile-scope dependency in test-support can trip it, as with `slf4j-api`.
- **XML comments can't contain `--`**, e.g. `--release`, or the POM won't parse.
- **Module layout:** `*Test` = Docker-free (surefire); `*IT` = Docker (failsafe). JaCoCo checks merged coverage. The library's branch coverage is 81.9% against a 0.80 floor, which is a thin margin.

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
| 1 | New core: vendor-neutral `CatalogSnapshot` model + `MetadataReader` SPI; PostgreSQL reader on `pg_catalog` with bulk per-schema queries; public record catalog model; catalog-dump golden; second-vendor spike; least-privilege + query-count ITs; corrected fixtures | IN PROGRESS (step table reviewed; 1.1 next) |
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

### Phase 0 completion review (`tech-lead-reviewer`, 2026-11-26): carry-forward items

The review found no behaviour changes. `sonar.skip` was added to test-support and two inaccurate export TODOs were reworded. Open items, by phase:

**Phase 1 (decide in the Phase 1 plan before coding)**
- **Test-support can't depend on the library** (that would be a reactor cycle). Hand-built `CatalogSnapshot` fixtures and the catalog-dump writer/comparator can't live there. Options:
  - (a) the dump writer as an internal class in library main, which also helps debugging
  - (b) a library `test-jar` that the CLI consumes
- **Goldens stay in test-support.** Library tests run on the module path with test resources patched into the named module. `TestResources` runs in the unnamed module, so it can't read library-local resources in encapsulated packages. Library-local resources would have to be read with the test class's own `getResourceAsStream`.
- **No package overlap.** Never put a test-support class in a package the library owns, e.g. `…catalog`: it would be silently unloadable.
- **Keep the `-Pdocs` internal-site `--add-exports` list in sync.** It sits at `db-documenter/pom.xml` (~L199-243). Add or remove packages in the same commit that adds or deletes them.
- **Export the new public `catalog` package in `module-info.java`.**

**Phase 3**
- **`requires transitive java.sql` in the library `module-info`:** the public API exposes `SQLException`, and soon `DataSource`/`Connection`.
- **Qualified exports:**
  - `internal.exceptions` goes with the public exception.
  - `internal.utils` and `internal.validation` go only when the CLI stops calling `LogUtils`/`Validators`.
  - Dropping `LogUtils` from the CLI also retires `findsecbugs-taint.txt`.

**Phase 5**
- **Inherited `url`/`scm` paths:** child POMs inherit them with the artifactId appended, which produces 404s. Set `child.project.url.inherit.append.path="false"` and the `child.scm.*.inherit.append.path="false"` attributes. Publish the parent; don't flatten it away. Add `developers`.
- **Keep test-support out of the publish.** Verify that `central-publishing-maven-plugin` skips it: `maven.deploy.skip` isn't enough, and `skipPublishing` may need to be plugin config rather than a property. The parent's `dependencyManagement` names test-support, which is harmless.
- **Shade filters:** also exclude `META-INF/versions/*/module-info.class` (picocli's), and `META-INF/*.SF`, `*.DSA`, `*.RSA` before Phase 4's drivers land.
- **If the `jar-with-dependencies` classifier changes,** update all of these in one commit: Dockerfile `COPY`, `.dockerignore`, `ApplicationDockerIT`, and the `release.yml` jar globs and paths.

**Phase 7**
- **Bytecode version check:** `extra-enforcer-rules` `enforceBytecodeVersion` with `maxJdkVersion=21`, scoped to the library, in addition to the JDK 21 runtime job.
- **Maven wrapper:** add `distributionSha256Sum`. Consider a `./mvnw -v` warm-up before the `release.yml` version-capture step, because that step makes the first wrapper call in the job.
- **Maven 4:** `${maven.multiModuleProjectDirectory}` becomes `${session.rootDirectory}`.
- **If JUnit parallel execution is ever enabled,** mark `MainIT`/`MainTest` `@Isolated`, because they swap `System.err`.

### Phase 1 steps

Each step ends with a green `mvn -B clean verify` and its own commit. The step table was drafted
2026-11-26 and reviewed by `tech-lead-reviewer` the same day. Its findings (B = blocking,
S = should-fix, N = nice-to-have) and the user's answers are folded in below.

**User decisions (2026-11-26):**

| Topic | Decision |
|---|---|
| Visibility | **Only tables the user can SELECT** are documented. A table is included when the role has `USAGE` on its schema (`has_schema_privilege`) and `SELECT` on the table or any of its columns (`has_any_column_privilege`), and then all its columns are shown. FKs into invisible tables keep their target `QualifiedName`, the same as out-of-scope targets. This matches MySQL/SQL Server, whose catalogs already filter by privilege. Document it in the README's privilege section (Phase 3). |
| Minimum PostgreSQL | **15+** (14 reached end of life on 2026-11-12). Phase 1 tests on 15 and 18. Fixture syntax must parse on 15. |
| Unique key kind | **Dropped.** No constraint-vs-index distinction, because MySQL can't make it. The model has separate `PrimaryKey` and `UniqueKey` types instead of a kind on `Key`. |

**Phase 1 decisions:**
- **The old pipeline stays live for all of Phase 1.** `DbDocumenter` keeps using the
  `information_schema` builders and the PUML goldens don't change. The new reader is proven by catalog
  dumps and by a `DatabaseMetaData` oracle (1.5b). Phase 2 switches `DbDocumenter` to
  `MetadataReader` and the new renderer, regenerates the PUML goldens once, and deletes the old
  `internal.{builder,mapper,queries,models,formatter,connection}` layers. Users see the C1/C2 fixes
  in Phase 2.
- **Dump writer:** an internal class in library main, `internal.catalog.CatalogDumpWriter`. This
  needs no `test-jar`. The CLI can expose it later as a debug flag, and the Phase 4 cross-vendor ITs
  reuse it.
- **Hand-built `CatalogSnapshot` fixtures** live in library **test** sources (patched into the named
  module). Add a `test-jar` only if the CLI ever needs them.
- **Test-support holds only code that needs nothing beyond `java.sql`** (S14):
  - `QueryCountingConnection`
  - string-level dump comparison
  - SQL fixtures and goldens, read via `TestResources`. Catalog dumps go in
    `/<vendor>/<fixture>/…-expected-catalog.txt`, and the new PG fixture in
    `/postgresql/catalog-edge-cases/`.

  Never put a test-support class under `…catalog` or `…internal.*`.
- **Packages:**
  - `dev.shanelucy.dbdocumenter.catalog`: public and exported
  - `internal.reader`: `MetadataReader`, the `MetadataReaders` registry, and the `ReadRequest` record
    (N1)
  - `internal.reader.postgresql`: `PgCatalogReader`
  - `internal.catalog`: the dump writer

  Each commit that adds a package also updates the `-Pdocs` `--add-exports` list, or the list is
  replaced (see 1.1).
- **Model invariants:**
  - **Total canonical order, set in the compact constructors (B3):**
    - Schemas, tables and enums sort by name in Java `String` order.
    - Columns keep list order. There is no `ordinal` component (B4): list position is the column
      position, readers `ORDER BY` the vendor ordinal, and the dump never prints a vendor ordinal.
    - Key and FK columns keep key position.
    - Unique keys sort by column list (lexicographic), then name (empty first).
    - FKs sort by local columns, then referenced table, then referenced columns, then actions, then
      name.
  - **Uniqueness is validated where it really holds:**
    - table names within a schema
    - column names within a table
    - enum names within a schema
    - every table's `QualifiedName.schema` equals the schema that contains it

    There is no cross-table referential validation, because FKs can dangle (SQLite, out-of-scope or
    invisible targets).
  - **The public records document `@throws IllegalArgumentException`** and never name the internal
    `ValidationException` (S12). Builder setters take plain values, and `Optional` appears only in
    components and accessors.
- **Reader contract:**
  - `MetadataReader.read(Connection, ReadRequest)` never closes the connection and never changes
    autocommit, isolation or `search_path` on it.
  - It returns only schemas that exist and are visible. H3 validation is Phase 3.
  - Rows that become orphans through concurrent DDL between queries are dropped and logged, not
    thrown (S10).
  - An unsupported product throws `SQLFeatureNotSupportedException` until the public exception
    exists (Phase 3).
- **PG reader:**
  - Bulk queries per run, not per schema. Each query filters `nspname = ANY(?)`, so a run issues a
    fixed number of statements (≈6, ceiling 8) whatever the schema or table count.
  - **Types (B1):** `format_type` only for types in `pg_catalog`, because elsewhere its output
    depends on `search_path` and it quotes identifiers itself. For enums, domains, composites and
    ranges, and arrays of them via `typelem`:
    - `userType = QualifiedName(nspname, typname)` from the raw catalog columns
    - `displayText` = bare `typname`, plus `[]` for arrays

    For an array, `userType` is the element type, and for a domain it is the domain. Never cast to a
    `reg*` type for output.
- **Scope rules (S7):**
  - FKs into tables that are out of scope or invisible are kept, and the target is absent from the
    snapshot.
  - Only enums in the requested schemas are included, so a column's `userType` may dangle. The
    renderer handles that in Phase 2.
  - Legacy `INHERITS` children are ordinary tables and are included. Partitions are excluded.
- **MySQL inline `ENUM`/`SET`:** decided in 1.2, between `displayText = COLUMN_TYPE` verbatim and a
  `DataType.inlineValues` component. Adding the component after 1.0 is a breaking change, so record
  the reasoning either way.

| Step | Work | Status |
|---|---|---|
| 1.1 | **Catalog model.** <br>• Public records and builders in the exported `catalog` package: `CatalogSnapshot`, `Schema`, `QualifiedName`, `Table`, `Column`, `DataType`, `PrimaryKey`, `UniqueKey`, `ForeignKey` (ordered column pairs), `ReferentialAction`, `EnumType`. <br>• `ReferentialAction` has all five values (N3). <br>• Total comparators, uniqueness and containment validation per the invariants. <br>• Add `exports …catalog` to `module-info`. <br>• Unit tests are kept minimal until 1.2 settles the model (S5). Write fresh builders and don't copy the old `internal.models.db` ones: CPD and Sonar duplication (S3). <br>• **Fix the `-Pdocs` list (S13).** It lists 4 packages that don't exist (`internal.api`, `internal.connection.impl`, `internal.impl`, `internal.models.doc`) and misses 5 that do (`internal.formatter{,.api,.impl.entity,.impl.multiplicity}`, `internal.models.db.postgresql`). Prefer replacing it with `--show-module-contents=all --show-packages=all` if javadoc accepts that. Run `./mvnw -Pdocs verify` via `maven-architect`. | NOT STARTED |
| 1.2 | **Second-vendor spike (SQLite) and paper mapping.** <br>• **Run outside the reactor (S5):** in the scratchpad, compile against `db-documenter/target/classes` plus the `sqlite-jdbc` jar from `~/.m2`. No pom change, and `slf4j-api` can't break dependency convergence. Keep the spike code on a local unpushed branch to seed the Phase 4 reader. <br>• **SQLite checklist:** <br>  – empty declared type (can `displayText` be blank?) <br>  – `REFERENCES parent` with no column list (resolve the target's PK) <br>  – dangling FKs <br>  – FK names exposed by no pragma (`sqlite_master.sql` parsing only), so names become `Optional` <br>  – `WITHOUT ROWID` and `INTEGER PRIMARY KEY` <br>  – hidden generated columns (`table_xinfo`) <br>  – `main`/`temp`/attached databases mapped to `schema` <br>• **Written deliverable:** a one-page table mapping each model component to its MySQL `information_schema` and SQL Server `sys.*` source, noting where a value can be empty or unknown. Covers SQL Server's random-hex auto-names and the inline `ENUM`/`SET` decision. <br>• Record findings here, then finish the 1.1 records and their full unit tests. Commit the model, tests and notes only. | NOT STARTED |
| 1.3 | **Reader SPI, registry, dump writer, query counter.** <br>• Internal `MetadataReader` and `ReadRequest`. <br>• `MetadataReaders` is a static registry keyed on `getDatabaseProductName()`, with a version hook for MariaDB. It is **empty** until 1.5a (S1) and throws `SQLFeatureNotSupportedException`, which is unit-tested. <br>• `CatalogDumpWriter` takes an options record, with only verbatim mode implemented (S6). Phase 4's neutral mode will omit constraint names, normalise types per vendor, drop `enum` lines and fold case per `Dialect`. <br>• Dump format (B5): <br>  – no product name or version <br>  – every line self-contained with its full path, e.g. `fk auth.user_mfa(user_id) -> auth.users(id) on_delete=cascade on_update=no_action name=user_mfa_user_id_fkey` <br>  – identifiers bare when they match `[a-z0-9_]+`, otherwise quoted <br>  – every quoted token (identifiers, enum labels, `displayText`) escapes `\n \r \t \\ "` and non-printables <br>  – LF line endings and a trailing newline <br>  – add `.gitattributes`: `*.txt text eol=lf` <br>• Unit tests against hand-built snapshots. <br>• `QueryCountingConnection` in test-support **wraps returned `Statement`s and counts every `execute*`** (S8). It has its own unit test in the library's tests: one prepared statement executed 3 times counts 3. Document the `getMetaData()` blind spot. | NOT STARTED |
| 1.4 | **Edge-case PG fixture and hand-written expected dump.** <br>`database-schema-architect` writes both from the DDL, never generated, and not by the reader's author (S2). SQL must parse on PG 15. Commit the fixture and dump only. The IT arrives in 1.5a (N4). <br>**Cases:** <br>• composite FK with column order different from the referenced key's declaration <br>• auto-named FKs (`orders_uid_fkey`) in two sibling schemas with same-named tables (C1) <br>• FKs *to* and *from* a partitioned table; partitions excluded <br>• FK to a unique non-PK column, and FK to a standalone unique index (B2) <br>• unique constraint and unique index, each with `INCLUDE (…)` (B2) <br>• partial and expression unique indexes (excluded) <br>• composite unique; table without a PK <br>• `NOT VALID` FK; deferrable unique (N7) <br>• identifiers with spaces, `"`, `-`, `$`, mixed case and an embedded newline <br>• `int4[]`, `varchar(20)[]`, `numeric(10,2)` <br>• cross-schema enum, enum array, domain over an enum (S7) <br>• dropped column; legacy `INHERITS` child <br>• FK to a schema outside the requested set <br>• all five referential actions | NOT STARTED |
| 1.5a | **`PgCatalogReader` (bulk `pg_catalog`).** <br>• **Tables:** `relkind IN ('r','p')`, `NOT relispartition`, plus the visibility predicate. <br>• **Columns:** `attnum > 0`, `NOT attisdropped`, B1 type handling, ordered by `attnum`. <br>• **Constraints:** `contype IN ('p','u','f')`, `conparentid = 0`, `unnest(conkey, confkey) WITH ORDINALITY`. <br>• **Unique indexes (B2):** `indisunique`, `indisvalid`, `indpred IS NULL`, no `indkey` entry of 0, only the first `indnkeyatts` keys, and `NOT EXISTS (… c.conindid = i.indexrelid AND c.conrelid = i.indrelid AND c.contype IN ('p','u'))`. <br>• **Enums:** labels by `enumsortorder`. <br>• The PG-code → `ReferentialAction` mapping is a unit-tested function that throws on unknown codes (N3). <br>• Everything parameterised. Register PG in `MetadataReaders`. <br>• Make the `PostgresTestEnvironment` image configurable via a system property (S11). The edge-case IT is green on `postgres:15` and `postgres:18`. <br>• Use one shared container per IT class, with roles created idempotently (N5). | NOT STARTED |
| 1.5b | **Existing-fixture dumps and the oracle.** <br>• Generate catalog dumps for the three existing fixtures. An independent agent (`database-schema-architect`) reviews them line by line against the DDL and against the appendix's "Verified correct" list. The composite FK into `warehouse_locations` must appear exactly once. <br>• A test-only `DatabaseMetaData` oracle IT (S3) compares, on all fixtures: tables, column names and nullability, PK columns in key order, and FK column pairs. Known differences are allow-listed, e.g. pgjdbc listing partition FKs. It is reused in Phase 4. | NOT STARTED |
| 1.6 | **Least-privilege and query-count ITs.** <br>• **Visibility (C2):** each role's dump is asserted. <br>  – A role with `CONNECT`, `USAGE` and `SELECT` gets a dump identical to the owner's. <br>  – A role with column-level `SELECT` on one table sees that table with all its columns. <br>  – A role with `USAGE` and no table privileges sees no tables. <br>  – A role without `USAGE` sees no schema content. <br>  – A role with `ALTER ROLE … SET search_path = auth, public`, and a role named `auth`, each get a dump identical to the owner's (B1). <br>• **Query count (S8):** the generated 50-table and 2,000-table schemas include PKs, FKs, uniques, unique indexes and enum columns. They must issue the same number of executions, as must a 3-schema run and a 1-schema run, all ≤ 8. <br>• **Measure before tagging (S9):** if the 2,000-table case adds < 30 s to `verify`, it runs untagged. Otherwise tag it `scale` and exclude it via an overridable property, with a separate CI job (`maven-architect`). | NOT STARTED |
| 1.7 | **Phase close.** <br>• Run `./mvnw -Pdocs verify` again. <br>• JaCoCo margins stay above 0.80. <br>• `tech-lead-reviewer` completion review of the whole phase. <br>• Phase 2 carry-forward: <br>  – switching `DbDocumenter` and regenerating goldens is a separate commit from deleting the old layers <br>  – re-measure the branch margin after the deletion <br>  – watch CPD <br>  – the renderer owns qualification of `userType` and dangling targets <br>• Add to "Facts deliberately considered" (N2): column default, identity and generated; table kind; FK deferrable, `NOT VALID` and match type; `ON DELETE SET NULL (cols)`; comments; a vendor-extension map. <br>• Update the Phases table. | NOT STARTED |

**Phase 1 must NOT:**
- change `DbDocumenter` behaviour or any PUML golden
- delete the old pipeline
- add public API beyond the `catalog` records (`readCatalog()` and the public exception are Phase 3)
- add production dependencies

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
