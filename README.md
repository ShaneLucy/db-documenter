# db-documenter

A library that generates [PlantUML](https://plantuml.com/) entity-relationship diagrams by introspecting live database schemas.

---

## Installation

### Docker

```bash
docker pull ghcr.io/shanelucy/db-documenter:latest
```

### Maven

```xml
<dependency>
  <groupId>db.documenter</groupId>
  <artifactId>db-documenter</artifactId>
  <version>1.0.22</version>
</dependency>
```

---

## Usage

### Docker (Manual)

Use this when you want to generate a diagram for a database running on your local machine.

> **Linux:** Use `--network=host` with `--host=localhost` instead of `host.docker.internal`.

#### Write to a file (volume mount)

```bash
docker run --rm -v "$(pwd):/output" ghcr.io/shanelucy/db-documenter:latest \
  --host=host.docker.internal \
  --port=5432 \
  --database=myapp \
  --username=postgres \
  --password=secret \
  --schemas=public,audit \
  --output=/output/schema.puml
```

This writes the ERD to `schema.puml`. If any schema defines enums they are written to a separate
diagram next to it, `schema_enums.puml` (the name gets `_enums` before the extension). When there are
no enums, a stale `schema_enums.puml` from a previous run is deleted.

#### Write to stdout

```bash
docker run --rm ghcr.io/shanelucy/db-documenter:latest \
  --host=host.docker.internal \
  --database=myapp \
  --username=postgres \
  --password=secret \
  --schemas=public > schema.puml
```

Only the ERD is written to stdout. If the schemas define enums, a warning on stderr says the enum
diagram was not written; use `--output` to get it.

#### Linux (host network)

```bash
docker run --rm --network=host -v "$(pwd):/output" ghcr.io/shanelucy/db-documenter:latest \
  --host=localhost \
  --database=myapp \
  --username=postgres \
  --password=secret \
  --schemas=public \
  --output=/output/schema.puml
```

#### Disable SSL (for databases without SSL configured)

Pass `--ssl=false` to skip SSL negotiation:

```bash
docker run --rm ghcr.io/shanelucy/db-documenter:latest \
  --host=host.docker.internal \
  --database=myapp \
  --username=postgres \
  --password=secret \
  --schemas=public \
  --ssl=false > schema.puml
```

#### All options

```
--host=<host>       Database hostname (required)
--port=<port>       Database port (default: 5432)
--database=<name>   Database name (required)
--username=<user>   Database username (required)
--password=<pass>   Database password (omit to be prompted interactively)
--schemas=<list>    Comma-separated schema names (required)
--ssl=<bool>        Use SSL connection (default: true, pass --ssl=false to disable)
--output=<path>     Output file path (default: stdout)
--help              Show help message
--version           Print version
```

**Exit codes:** `0` success · `1` configuration error · `2` database error · `3` unexpected error

---

### Testcontainers

Use db-documenter to generate schema diagrams as part of an automated test suite. Connect it to your test database container via a shared Docker network.

**Pattern:** Both containers share a network. db-documenter connects to the database using the container's network alias and the internal port (5432), not the mapped host port. Check the exit code to detect failures.

#### Python

```python
from testcontainers.postgres import PostgresContainer
from testcontainers.core.container import DockerContainer
from testcontainers.core.network import Network

def test_generate_documentation(tmp_path):
    network = Network()
    network.create()

    with PostgresContainer("postgres:16") \
            .with_network(network) \
            .with_network_aliases("postgres") as postgres:

        # Run your migrations here

        doc_gen = (
            DockerContainer("ghcr.io/shanelucy/db-documenter:latest")
            .with_network(network)
            .with_volume_mapping(str(tmp_path), "/output", "rw")
            .with_command(
                "--host postgres "
                "--port 5432 "
                f"--database {postgres.dbname} "
                f"--username {postgres.username} "
                f"--password {postgres.password} "
                "--schemas public "
                "--ssl=false "
                "--output /output/schema.puml"
            )
        )
        doc_gen.start()
        exit_code = doc_gen.get_wrapped_container().wait()["StatusCode"]
        doc_gen.stop()

    network.remove()

    assert exit_code == 0
    assert (tmp_path / "schema.puml").exists()
```

#### Node.js

```javascript
const { PostgreSqlContainer, GenericContainer, Network } = require('testcontainers');
const { existsSync } = require('fs');
const path = require('path');

test('generate database documentation', async () => {
  const network = await new Network().start();

  const postgres = await new PostgreSqlContainer('postgres:16')
    .withNetwork(network)
    .withNetworkAliases('postgres')
    .start();

  // Run your migrations here

  const outputDir = __dirname;

  const docGen = await new GenericContainer('ghcr.io/shanelucy/db-documenter:latest')
    .withNetwork(network)
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
    .start();

  const exitCode = (await docGen.stop()).exitCode;

  expect(exitCode).toBe(0);
  expect(existsSync(path.join(outputDir, 'schema.puml'))).toBe(true);

  await postgres.stop();
  await network.stop();
});
```

#### Go

```go
func TestGenerateDocumentation(t *testing.T) {
    ctx := context.Background()

    network, _ := testcontainers.GenericNetwork(ctx, testcontainers.GenericNetworkRequest{
        NetworkRequest: testcontainers.NetworkRequest{Name: "db-docs-net"},
    })
    defer network.Remove(ctx)

    postgres, _ := testcontainers.GenericContainer(ctx, testcontainers.GenericContainerRequest{
        ContainerRequest: testcontainers.ContainerRequest{
            Image:          "postgres:16",
            Networks:       []string{"db-docs-net"},
            NetworkAliases: map[string][]string{"db-docs-net": {"postgres"}},
            Env: map[string]string{
                "POSTGRES_DB":       "testdb",
                "POSTGRES_USER":     "test",
                "POSTGRES_PASSWORD": "test",
            },
            WaitingFor: wait.ForLog("database system is ready to accept connections"),
        },
        Started: true,
    })
    defer postgres.Terminate(ctx)

    // Run your migrations here

    outputDir := t.TempDir()

    docGen, _ := testcontainers.GenericContainer(ctx, testcontainers.GenericContainerRequest{
        ContainerRequest: testcontainers.ContainerRequest{
            Image:    "ghcr.io/shanelucy/db-documenter:latest",
            Networks: []string{"db-docs-net"},
            Cmd: []string{
                "--host", "postgres",
                "--port", "5432",
                "--database", "testdb",
                "--username", "test",
                "--password", "test",
                "--schemas", "public",
                "--ssl=false",
                "--output", "/output/schema.puml",
            },
            Mounts:     testcontainers.Mounts(testcontainers.BindMount(outputDir, "/output")),
            WaitingFor: wait.ForExit(),
        },
        Started: true,
    })
    defer docGen.Terminate(ctx)

    state, _ := docGen.State(ctx)
    assert.Equal(t, 0, state.ExitCode)
    _, err := os.Stat(filepath.Join(outputDir, "schema.puml"))
    assert.NoError(t, err)
}
```

#### Java

```java
@Test
void generateDocumentation() throws Exception {
    try (Network network = Network.newNetwork();
         PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
                 .withNetwork(network)
                 .withNetworkAliases("postgres")) {

        postgres.start();

        // Run your migrations here

        Path outputDir = Files.createTempDirectory("db-docs");

        try (GenericContainer<?> docGen = new GenericContainer<>("ghcr.io/shanelucy/db-documenter:latest")
                .withNetwork(network)
                .withFileSystemBind(outputDir.toString(), "/output", BindMode.READ_WRITE)
                .withCommand(
                        "--host", "postgres",
                        "--port", "5432",
                        "--database", postgres.getDatabaseName(),
                        "--username", postgres.getUsername(),
                        "--password", postgres.getPassword(),
                        "--schemas", "public",
                        "--ssl=false",
                        "--output", "/output/schema.puml")
                .waitingFor(Wait.forExit(30))) {

            docGen.start();

            assertEquals(0, docGen.getCurrentContainerInfo().getState().getExitCodeLong());
            assertTrue(Files.exists(outputDir.resolve("schema.puml")));
        }
    }
}
```

---

## Database Support

The tables below document which schema features are supported for each database. This is a reference for both **consumers** evaluating db-documenter and **maintainers** tracking feature coverage across database implementations.

### Legend

| Symbol | Meaning |
|:------:|---------|
| ✅ | Fully supported |
| 🔜 | Planned |
| ❌ | Not supported |

---

### Schema Objects

Which top-level database objects are discovered and rendered.

| Feature | PostgreSQL |
|---------|:----------:|
| Regular tables | ✅ |
| Partitioned tables (with partition key) | ✅ |
| Partition children (listed under parent) | ✅ |
| Views | ✅ |
| Materialized views | ✅ |
| Enums | ✅ |
| Composite types | ✅ |
| Standalone sequences | ❌ |
| Non-constraint indexes | ❌ |
| Domain types | ❌ |
| Foreign tables (FDW) | ❌ |
| Functions / stored procedures | ❌ |
| Triggers | ❌ |

---

### Column Metadata

Which column-level properties are captured and included in the diagram output.

| Feature | PostgreSQL |
|---------|:----------:|
| Data type | ✅ |
| Nullability | ✅ |
| Primary key | ✅ |
| Foreign key | ✅ |
| Unique constraint (single-column) | ✅ |
| Unique constraint (composite, with constraint name) | ✅ |
| Auto-increment / sequence (`nextval`) | ✅ |
| Default value | ✅ |
| Check constraint | ✅ |
| Generated column | ✅ |
| Array type | ✅ |
| User-defined type (enum / composite reference) | ✅ |
| Character maximum length | ✅ |
| Numeric precision and scale | ✅ |
| Column comments | ❌ |
| Collation | ❌ |

---

### Relationships

How foreign key relationships are represented in the output diagram.

| Feature | PostgreSQL |
|---------|:----------:|
| Foreign key relationships | ✅ |
| Relationship cardinality | ✅ |
| Cross-schema relationships | ✅ |
| ON DELETE action | ✅ |
| ON UPDATE action | ✅ |
