# db-documenter — Project Completion Plan

Supersedes `ERD_REFACTORING_PLAN.md` (its file-by-file detail is still the reference for phases C–F,
with the corrections below). Reviewed by `tech-lead-reviewer` on 2026-11-26; findings folded in.

## Outcome (definition of done)

db-documenter is a focused **ERD generator**: it introspects PostgreSQL schemas and emits a PlantUML
ERD (tables, columns, PK/FK/UNIQUE markers, cardinality, cross-schema FKs) plus a **separate enum
diagram**. It is **release-ready**: a `vX.Y.Z` tag push verifies, publishes the Docker image to GHCR
and creates a GitHub Release with the CLI jar. Maven Central publishing is out of scope (deferred).

Done when:
1. `mvn clean verify` is green (unit + integration tests, Checkstyle, PMD, SpotBugs, NullAway).
2. Integration SQL fixtures (which still contain views, composite types, partitions) are silently
   ignored; the expected `.puml` golden file reflects the new output.
3. README describes actual behaviour (ERD + `_enums.puml`, Docker `--output` with volume mount,
   supported features, CLI options) — no references to unpublished Maven artifacts.
4. `release.yml` runs `mvn verify` + a jar smoke test and succeeds without Maven Central secrets.
5. One version source of truth: CLI `--version` reads the jar manifest; release fails if tag ≠ pom.
6. LICENSE file present.
7. Standards skills are consistent with each other and the codebase; null safety is compiler-enforced.
8. Final `tech-lead-reviewer` pass has no unresolved important findings.

## Working agreement

- Commit directly to `main` after each green phase. **Never push. No tags** (user tags + pushes).
- Gate per phase: `mvn spotless:apply`, `mvn test`, and update the IT golden file
  (`src/test/resources/postgresql/sql-agnostic-multiple-schema-postgres-expected-output.puml`) to the
  phase's new output. Run `mvn verify` (failsafe ITs) whenever Docker is available.
- "Don't modify fixtures" applies to the SQL fixtures only — the expected `.puml` must be updated.
- Production code follows `java-coding-standards` + `javadoc-standards`; tests follow
  `java-test-coding-standards`.
- Untracked legacy docs (`COLUMN_MAPPER_REFACTORING_PLAN.md`, `COMPREHENSIVE_TECHNICAL_REVIEW.md`,
  `ERD_REFACTORING_PLAN.md`, `RELEASE_SETUP_TODO.md`) are not committed or deleted — user decides.

## Phases (in execution order)

| # | Phase | Status |
|---|-------|--------|
| A | Reconcile coding-standards skills (docs only) | DONE |
| B | ERD Phase 1 (views/composites/mat-views removed) — verify | DONE |
| C | ERD Phase 2: constraint stereotypes, referential actions, `Column.isNullable` → compiles, green, **commit B+C** | DONE |
| D | ERD Phase 3: remove partition info | DONE |
| E | ERD Phase 4: Schema model cleanup verification | DONE |
| G | Null safety: Error Prone + NullAway in build (before new API is written) | DONE |
| F | ERD Phase 5: `DiagramResult`, enum diagram, CLI two-file output | DONE |
| H | Release readiness (workflow, version, manifest, README, LICENSE, git hygiene) | DONE |
| I | Final `tech-lead-reviewer` review; fix findings; final `mvn clean verify` | DONE |

### C. ERD Phase 2 — corrections to ERD_REFACTORING_PLAN.md
- **CRITICAL:** `ColumnMapper.java` (`mapUserDefinedTypes` ~L148 and `enrichWithForeignKeyConstraints`
  ~L181) rebuilds `Column` via the builder without `isNullable`. The builder's primitive `boolean`
  defaults to `false`, so every FK would silently render `||--|{`. Fix: carry `isNullable` through
  (prefer `withDataType()` / a `withConstraints()` wither over re-building), and make the builder
  fail fast if `isNullable` was never set (builder field `Boolean`, validated in `build()`).
  Add `ColumnMapperTest` cases asserting nullability is preserved through both methods.
- Fix the stale `Constraint.NULLABLE` example in `models/db/package-info.java`.
- Delete `ReferentialAction.java` (still present).

### G. Null safety (moved ahead of F)
- `module-info.java` is already `@NullMarked` → no per-package annotations needed.
- `maven-architect` adds Error Prone + NullAway to the compiler plugin:
  - Error Prone release with documented JDK 25 support.
  - Compiler uses `<fork>true</fork>` → javac `--add-exports/--add-opens jdk.compiler/...` go in
    `compilerArgs` as `-J...`; plus `-XDcompilePolicy=simple`, `--should-stop=ifError=FLOW`; the
    whole `-Xplugin:ErrorProne ...` string is a single `<arg>`.
  - NullAway: `-XepOpt:NullAway:OnlyNullMarked=true`; JSpecify mode off initially.
  - Disable NullAway for test compilation.
  - `requires org.jspecify` → `requires static org.jspecify` (provided scope).
  - Policy for non-nullness Error Prone ERROR checks: fix if trivial; otherwise disable that check by
    name with a comment justifying it. Don't let G grow unbounded.
- Picocli-injected fields in `Main`: `@Nullable` (password, output) or `@SuppressWarnings("NullAway.Init")`.
- Remove redundant `@NonNull` usages.
- Convert the 9 test files using `import static ...Assertions.*` to individual static imports.

### F. ERD Phase 5 — corrections to ERD_REFACTORING_PLAN.md
- `DiagramResult` (exported `db.documenter` package): validated compact constructor; no empty-string
  sentinel in the API — expose `Optional<String> enumDiagram()` (or `hasEnumDiagram()`).
- Enum diagram: just omit `hide stereotypes` (`show` is PlantUML default). Only emit `package`
  blocks for schemas that have enums. (Consider PlantUML native `enum` keyword — optional.)
- `_enums` path: operate on `getFileName()` only; `lastIndexOf('.')`; handle no extension
  (`out` → `out_enums`) and dotfiles; resolve against `getParent()`.
- If no enums: don't write `_enums.puml`; if a stale one exists from a previous run, delete it
  (document the behaviour). Define the exit code when ERD write succeeds but enum write fails
  (treat as failure, exit 3 → document).
- Stdout mode: ERD to stdout; stderr warning if enums were not written.

### H. Release readiness
- `release.yml`: remove `publish-maven` job; `create-release` needs only `publish-docker`; build job
  runs `mvn -B verify` (not `-DskipTests`) + smoke test `java -jar …-jar-with-dependencies.jar --version`;
  fail if tag version ≠ pom version.
- Version: picocli `IVersionProvider` reading `Implementation-Version` from the manifest (shade
  manifest entries); drop hardcoded `1.0.22` in `Main`.
- `pom.xml:320` thin-jar `mainClass` → `db.documenter.cli.Main` (via `maven-architect`).
- `dependency-reduced-pom.xml`: `git rm --cached` + `.gitignore`.
- LICENSE file: **Apache-2.0** (user decision); add `<licenses>` block to pom.
- README: drop Maven install section; document ERD + `_enums.puml`, Docker `--output` volume mount
  (container runs as non-root `appuser` → document `--user "$(id -u):$(id -g)"` if needed), the stdout
  limitation for enums, and which object types are intentionally ignored.
- Add IT: file output via Docker writes both ERD and enum files. (DONE in phase F — runs as host uid:gid)

### I. Known follow-ups (from phase C test review)
- `ForeignKeyMapper.enrichWithNullability` matches source column with case-sensitive `equals` while
  `ColumnMapper.enrichWithForeignKeyConstraints` uses `equalsIgnoreCase` → make consistent.
- `ForeignKey.Builder` silently defaults `isNullable` → same fail-fast treatment as `Column.Builder`.
- (from phase G) Composite formatters silently treat a null formatter list as empty → validate instead.
- (from phase G) `TableMapper` does `primaryKey.orElse(null)` only for `Table.Builder` to re-wrap it →
  let `Table.Builder.primaryKey` accept `Optional<PrimaryKey>` (or add an overload).
- (from phase G verify) Clear the 7 Error Prone warnings: 6 `MissingSummary` on `ForeignKey.Builder`
  setter Javadoc, 1 `InlineFormatString` in `PostgresConnectionManager:15`.
- (from phase F) Skill note: SpotBugs can't see that `Validators.*` returns non-null — don't chain
  calls on its result (`isNotNull(x).toString()`); use an explicit null check for dereferences.

## Out-of-scope follow-ups for the user (from devops-lead, phase H)
1. Pin GitHub Actions to commit SHAs (esp. `softprops/action-gh-release`, which has `contents: write`).
2. Skip the `{{major}}` image tag for `v0.x` releases (publishes a meaningless `:0`).
3. Smoke-test the exact image before pushing (`load: true` build → `docker run … --version` → push).
4. `build.yaml`: make the Sonar goal conditional on `SONAR_TOKEN` (fork PRs fail today); add a
   `concurrency` group.
5. Pin `eclipse-temurin:25-jre-alpine` by digest/patch; enable Dependabot (docker + github-actions).
6. Protect `v*` tags / require tagged commits to be on `main`.
7. Maven Central publishing (needs groupId decision, Sonatype account, GPG) — see `RELEASE_SETUP_TODO.md`.

## Status (2026-11-26)
All phases complete. `mvn clean verify` green at HEAD: 451 unit tests, 2 Docker ITs, Error Prone/NullAway
(0 warnings), SpotBugs, Checkstyle, PMD/CPD, modernizer, jacoco (branch 0.83). Ready to tag `v0.2.0`.
Known limitation (documented in README): PlantUML-special enum labels are not escaped.

## Environment notes
- Docker Desktop WSL integration enabled 2026-11-26 (server 28.3.2) → ITs run locally. Gate before
  each commit: `mvn verify` (via `maven-architect`, per CLAUDE.md) incl. failsafe ITs.
