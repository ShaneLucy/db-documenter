# db-documenter

Generates [PlantUML](https://plantuml.com/) entity-relationship diagrams (ERDs) by introspecting a
live PostgreSQL database.

Point it at one or more schemas and it produces:

- **`schema.puml`**: the ERD, with tables, columns, primary/foreign/unique keys and relationship
  cardinality, including foreign keys that cross schemas.
- **`schema_enums.puml`**: a separate diagram of the enum types, written only when at least one
  documented schema defines an enum.

db-documenter deliberately draws **ERDs only**. It doesn't try to be a full physical data model, so
views, defaults, check constraints and similar details are left out (see [What's in the diagram](#whats-in-the-diagram)).

> **Status: pre-1.0.** The first release will be **1.0.0**. Until then, no versioned image or
> release jar is published, and the instructions below describe how they will be distributed. To
> use it now, [build from source](#building-from-source).

---

## Installation

### Docker (recommended)

```bash
docker pull ghcr.io/shanelucy/db-documenter:latest
```

Images are tagged `latest`, the full version (e.g. `1.0.0`), `major.minor` (e.g. `1.0`) and the
short commit SHA. Pin a full version in CI so you get reproducible output.

### Standalone jar

Every [GitHub Release](https://github.com/ShaneLucy/db-documenter/releases) includes
`db-documenter-<version>-cli.jar`. It needs Java 25:

```bash
java -jar db-documenter-1.0.0-cli.jar --help
```

---

## Usage

### Write both diagrams to files

```bash
docker run --rm \
  --user "$(id -u):$(id -g)" \
  -v "$(pwd):/output" \
  ghcr.io/shanelucy/db-documenter:latest \
  --host=host.docker.internal \
  --database=myapp \
  --username=postgres \
  --password=secret \
  --schemas=public,audit \
  --output=/output/schema.puml
```

This writes `schema.puml` and, if any of the schemas define enums, `schema_enums.puml`.

> **Why `--user`?** The image runs as an unprivileged user. Running it as your own user id lets it
> write into the mounted directory, and the generated files end up owned by you rather than by a
> container account.

> **Linux:** `host.docker.internal` isn't available by default. Add `--network=host` and use
> `--host=localhost`.

### Write the ERD to stdout

```bash
docker run --rm ghcr.io/shanelucy/db-documenter:latest \
  --host=host.docker.internal \
  --database=myapp \
  --username=postgres \
  --password=secret \
  --schemas=public > schema.puml
```

Only the ERD goes to stdout. If the schemas contain enums, a one-line warning is printed to stderr,
and you need `--output` to get the enum diagram as well.

### Output file naming

The enum diagram's name comes from the `--output` file name, with `_enums` inserted before the
extension:

| `--output`                | Enum diagram                    |
|---------------------------|---------------------------------|
| `schema.puml`             | `schema_enums.puml`             |
| `docs/erd`                | `docs/erd_enums`                |
| `out/v1.2/model.puml`     | `out/v1.2/model_enums.puml`     |

If no documented schema has enums, no enum file is written. An enum diagram left over from a
previous run is deleted, so the two files never come from different runs. A file is treated as
db-documenter output only if it starts with `@startuml`. Any other file at that path, including a
directory, is left alone and you get a warning on stderr.

### Options

```
--host=<host>       Database hostname (required)
--port=<port>       Database port (default: 5432)
--database=<name>   Database name (required)
--username=<user>   Database username (required)
--password=<pass>   Database password (omit the value to be prompted; needs `docker run -it`)
--schemas=<list>    Comma-separated schema names (required)
--ssl=<bool>        Use SSL (default: true; pass --ssl=false for databases without SSL)
--output=<path>     ERD output file; the enum diagram is written alongside it (default: stdout)
-h, --help          Show help
-V, --version       Print the version
```

### Exit codes

| Code | Meaning |
|:----:|---------|
| `0`  | Success |
| `1`  | Invalid command line: a missing, blank or malformed option (e.g. `--port=abc`) |
| `2`  | Database error, such as a refused connection or failed authentication |
| `3`  | Unexpected error, including failure to write or delete an output file. If the ERD was written and the enum file then failed, the ERD file is left in place. |

---

## Generating diagrams in your test suite

You can run db-documenter against the database container in your own tests. Both containers share a
Docker network, and db-documenter connects using the database's network alias and its **internal**
port (`5432`), not the port mapped to the host. As with `docker run`, run the container as your user
so it can write to the mounted directory.

<details>
<summary><b>Python</b> (testcontainers-python)</summary>

```python
import os

from testcontainers.core.container import DockerContainer
from testcontainers.core.network import Network
from testcontainers.postgres import PostgresContainer


def test_generate_documentation(tmp_path):
    with Network() as network, PostgresContainer("postgres:16") \
            .with_network(network) \
            .with_network_aliases("postgres") as postgres:

        # Run your migrations here

        doc_gen = (
            DockerContainer("ghcr.io/shanelucy/db-documenter:latest")
            .with_network(network)
            .with_kwargs(user=f"{os.getuid()}:{os.getgid()}")
            .with_volume_mapping(str(tmp_path), "/output", "rw")
            .with_command(
                "--host postgres --port 5432 "
                f"--database {postgres.dbname} "
                f"--username {postgres.username} "
                f"--password {postgres.password} "
                "--schemas public --ssl=false "
                "--output /output/schema.puml"
            )
        )
        doc_gen.start()
        exit_code = doc_gen.get_wrapped_container().wait()["StatusCode"]
        doc_gen.stop()

    assert exit_code == 0
    assert (tmp_path / "schema.puml").exists()
```

</details>

<details>
<summary><b>Node.js</b> (testcontainers-node)</summary>

```javascript
const { PostgreSqlContainer } = require('@testcontainers/postgresql');
const { GenericContainer, Network, Wait } = require('testcontainers');
const { existsSync, mkdtempSync } = require('fs');
const os = require('os');
const path = require('path');

test('generate database documentation', async () => {
  const network = await new Network().start();
  const postgres = await new PostgreSqlContainer('postgres:16')
    .withNetwork(network)
    .withNetworkAliases('postgres')
    .start();

  // Run your migrations here

  const outputDir = mkdtempSync(path.join(os.tmpdir(), 'db-docs-'));

  const docGen = await new GenericContainer('ghcr.io/shanelucy/db-documenter:latest')
    .withNetwork(network)
    .withUser(`${process.getuid()}:${process.getgid()}`)
    .withBindMounts([{ source: outputDir, target: '/output', mode: 'rw' }])
    .withCommand([
      '--host', 'postgres',
      '--port', '5432',
      '--database', postgres.getDatabase(),
      '--username', postgres.getUsername(),
      '--password', postgres.getPassword(),
      '--schemas', 'public',
      '--ssl=false',
      '--output', '/output/schema.puml',
    ])
    .withWaitStrategy(Wait.forOneShotStartup())
    .start();
  await docGen.stop();

  expect(existsSync(path.join(outputDir, 'schema.puml'))).toBe(true);

  await postgres.stop();
  await network.stop();
});
```

</details>

<details>
<summary><b>Go</b> (testcontainers-go)</summary>

```go
func TestGenerateDocumentation(t *testing.T) {
    ctx := context.Background()

    net, err := network.New(ctx)
    require.NoError(t, err)
    defer net.Remove(ctx)

    pg, err := postgres.Run(ctx, "postgres:16",
        postgres.WithDatabase("testdb"),
        postgres.WithUsername("test"),
        postgres.WithPassword("test"),
        network.WithNetwork([]string{"postgres"}, net),
        postgres.BasicWaitStrategies(),
    )
    require.NoError(t, err)
    defer pg.Terminate(ctx)

    // Run your migrations here

    outputDir := t.TempDir()

    docGen, err := testcontainers.GenericContainer(ctx, testcontainers.GenericContainerRequest{
        ContainerRequest: testcontainers.ContainerRequest{
            Image:    "ghcr.io/shanelucy/db-documenter:latest",
            Networks: []string{net.Name},
            User:     fmt.Sprintf("%d:%d", os.Getuid(), os.Getgid()),
            Cmd: []string{
                "--host", "postgres", "--port", "5432",
                "--database", "testdb", "--username", "test", "--password", "test",
                "--schemas", "public", "--ssl=false",
                "--output", "/output/schema.puml",
            },
            Mounts:     testcontainers.Mounts(testcontainers.BindMount(outputDir, "/output")),
            WaitingFor: wait.ForExit(),
        },
        Started: true,
    })
    require.NoError(t, err)
    defer docGen.Terminate(ctx)

    state, err := docGen.State(ctx)
    require.NoError(t, err)
    assert.Equal(t, 0, state.ExitCode)
    assert.FileExists(t, filepath.Join(outputDir, "schema.puml"))
}
```

</details>

<details>
<summary><b>Java</b> (Testcontainers for Java)</summary>

```java
@Test
void generateDocumentation(@TempDir final Path outputDir) throws Exception {
  final var user = Files.getAttribute(outputDir, "unix:uid") + ":" + Files.getAttribute(outputDir, "unix:gid");

  try (var network = Network.newNetwork();
      var postgres = new PostgreSQLContainer<>("postgres:16")
          .withNetwork(network)
          .withNetworkAliases("postgres")) {
    postgres.start();

    // Run your migrations here

    try (var docGen = new GenericContainer<>("ghcr.io/shanelucy/db-documenter:latest")
        .withNetwork(network)
        .withCreateContainerCmdModifier(cmd -> cmd.withUser(user))
        .withFileSystemBind(outputDir.toString(), "/output", BindMode.READ_WRITE)
        .withCommand(
            "--host", "postgres", "--port", "5432",
            "--database", postgres.getDatabaseName(),
            "--username", postgres.getUsername(),
            "--password", postgres.getPassword(),
            "--schemas", "public", "--ssl=false",
            "--output", "/output/schema.puml")
        .withStartupCheckStrategy(new OneShotStartupCheckStrategy())) {
      docGen.start();

      assertTrue(Files.exists(outputDir.resolve("schema.puml")));
    }
  }
}
```

</details>

---

## What's in the diagram

db-documenter currently supports **PostgreSQL**. The codebase is structured so that other databases
can be added later.

### Included

| Feature | Notes |
|---------|-------|
| Tables | Regular tables, plus partitioned *parent* tables, which render as ordinary entities. Child partitions are left out. |
| Columns | Name, data type (including arrays, `numeric(p,s)`, character length and enum type names) |
| Primary keys | Listed first and highlighted |
| Foreign keys | `<<FK>>` marker, with a relationship line to the target table |
| Unique constraints | `<<UNIQUE>>` marker, for single- and multi-column constraints |
| Cardinality | `\|\|--o{` when the FK column is nullable, `\|\|--\|{` when it is `NOT NULL` |
| Cross-schema relationships | Foreign keys and enum references between documented schemas |
| Enums | Drawn in the separate `_enums` diagram |

### Intentionally excluded

These belong in a logical or physical data model rather than an ERD. They are **ignored**, not
rejected, so databases that use them are still documented.

- Views and materialized views
- Composite types, domains, sequences, functions, triggers and indexes
- Partition keys and child partitions
- Column defaults, `CHECK` constraints, generated and identity columns, and comments
- `ON DELETE` / `ON UPDATE` referential actions
- Unique constraint names

### Known limitations

- Enum labels are written to the diagram as-is. A label that is itself PlantUML syntax, such as
  `--`, `..`, `==` or `}`, can break the rendering of the enum diagram.

---

## Building from source

Requirements: JDK 25, Maven 3.8+, and Docker for the integration tests.

```bash
./mvnw clean verify
```

The repository is a Maven reactor: the root `pom.xml` is the parent (`db-documenter-parent`) and the
code lives in three modules: `db-documenter/` (the library), `db-documenter-cli/` (the command-line
interface) and `db-documenter-test-support/` (shared test helpers, never published). The runnable fat JAR is built to
`db-documenter-cli/target/db-documenter-cli-<version>-jar-with-dependencies.jar`. Run `./mvnw -Pdocs -DskipTests verify`
to also generate the public and contributor Javadoc sites under `db-documenter/target/site/`.

The `verify` build runs:
- unit tests and Testcontainers integration tests, including one that runs the Docker image
- Error Prone with NullAway for compile-time null safety
- SpotBugs, Checkstyle, PMD/CPD, Spotless and modernizer
- the JaCoCo coverage check

## License

[Apache License 2.0](LICENSE)
