package db.documenter.internal.queries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.PrimaryKey;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.queries.preparedstatements.PreparedStatementMapper;
import db.documenter.internal.queries.resultsets.ResultSetMapper;
import db.documenter.internal.test.helpers.PostgresTestEnvironment;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.*;

class QueryRunnerTest {

  private static final PostgresTestEnvironment POSTGRES_TEST_ENVIRONMENT =
      new PostgresTestEnvironment();
  private QueryRunner queryRunner;
  private static Connection connection;

  @BeforeAll
  static void containerSetUp() throws SQLException, IOException {
    POSTGRES_TEST_ENVIRONMENT.startContainer();
    connection = POSTGRES_TEST_ENVIRONMENT.getConnection();
    POSTGRES_TEST_ENVIRONMENT.initialiseDatabase(connection, "/single-schema/test-db.sql");
  }

  @BeforeEach
  void setUp() {
    queryRunner = new QueryRunner(new PreparedStatementMapper(), new ResultSetMapper(), connection);
  }

  @AfterAll
  static void containerClearDown() throws SQLException {
    connection.close();
    POSTGRES_TEST_ENVIRONMENT.stop();
  }

  @Nested
  class TableInfoTests {

    @Test
    void itReturnsAllTables() throws SQLException {
      final List<Table> tables = queryRunner.getTableInfo("public");
      final List<String> tableNames = tables.stream().map(Table::name).toList();

      assertTrue(
          tableNames.containsAll(
              List.of(
                  "app_user",
                  "role",
                  "user_role",
                  "address",
                  "product",
                  "category",
                  "product_category",
                  "customer_order",
                  "order_item",
                  "payment",
                  "audit_log")));
    }
  }

  @Nested
  class ColumnInfoTests {

    @Test
    void itReturnsColumns_appUser() throws SQLException {
      final Table appUser = Table.builder().schema("public").name("app_user").build();
      final List<Column> columns = queryRunner.getColumnInfo("public", appUser);
      final List<String> names = columns.stream().map(Column::name).toList();

      assertTrue(
          names.containsAll(
              List.of(
                  "id",
                  "username",
                  "email",
                  "display_name",
                  "birth_date",
                  "created_at",
                  "profile",
                  "is_active")));
    }

    @Test
    void itReturnsColumns_userRole() throws SQLException {
      final Table userRole = Table.builder().schema("public").name("user_role").build();
      final List<Column> columns = queryRunner.getColumnInfo("public", userRole);
      final List<String> names = columns.stream().map(Column::name).toList();

      assertTrue(names.containsAll(List.of("user_id", "role_id", "assigned_at", "assigned_by")));
    }

    @Test
    void itReturnsColumns_product() throws SQLException {
      final Table product = Table.builder().schema("public").name("product").build();
      final List<Column> columns = queryRunner.getColumnInfo("public", product);
      final List<String> names = columns.stream().map(Column::name).toList();

      assertTrue(
          names.containsAll(
              List.of(
                  "id",
                  "sku",
                  "name",
                  "description",
                  "price",
                  "attributes",
                  "created_at",
                  "stock")));
    }
  }

  @Nested
  class PrimaryKeyTests {

    @Test
    void itReturnsPrimaryKey_appUser() throws SQLException {
      final Table appUser = Table.builder().schema("public").name("app_user").build();
      final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo("public", appUser);

      assertEquals("app_user_pkey", primaryKey.constraintName());
      assertEquals(1, primaryKey.columnNames().size());
      assertEquals("id", primaryKey.columnNames().getFirst());
    }

    @Test
    void itReturnsCompositePrimaryKey_userRole() throws SQLException {
      final Table userRole = Table.builder().schema("public").name("user_role").build();
      final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo("public", userRole);

      assertEquals("user_role_pkey", primaryKey.constraintName());
      assertEquals(2, primaryKey.columnNames().size());
      assertTrue(primaryKey.columnNames().containsAll(List.of("user_id", "role_id")));
    }

    @Test
    void itReturnsPrimaryKey_productCategory() throws SQLException {
      final Table productCategory =
          Table.builder().schema("public").name("product_category").build();
      final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo("public", productCategory);

      assertEquals("product_category_pkey", primaryKey.constraintName());
      assertEquals(2, primaryKey.columnNames().size());
      assertTrue(primaryKey.columnNames().containsAll(List.of("product_id", "category_id")));
    }
  }

  @Nested
  class ForeignKeyTests {

    @Test
    void itReturnsForeignKey_address() throws SQLException {
      final Table address = Table.builder().schema("public").name("address").build();
      final List<ForeignKey> fks = queryRunner.getForeignKeyInfo("public", address);

      assertEquals(1, fks.size());
      final ForeignKey fk = fks.getFirst();
      assertEquals("address_user_id_fkey", fk.name());
      assertEquals("address", fk.sourceTable());
      assertEquals("user_id", fk.sourceColumn());
      assertEquals("app_user", fk.targetTable());
      assertEquals("id", fk.targetColumn());
    }

    @Test
    void itReturnsForeignKeys_userRole() throws SQLException {
      final Table userRole = Table.builder().schema("public").name("user_role").build();
      final List<ForeignKey> fks = queryRunner.getForeignKeyInfo("public", userRole);

      assertEquals(3, fks.size());
      final List<String> targetTables = fks.stream().map(ForeignKey::targetTable).toList();
      assertTrue(targetTables.containsAll(List.of("app_user", "role")));
    }

    @Test
    void itReturnsForeignKeys_orderItem() throws SQLException {
      final Table orderItem = Table.builder().schema("public").name("order_item").build();
      final List<ForeignKey> fks = queryRunner.getForeignKeyInfo("public", orderItem);

      assertEquals(2, fks.size());
      final List<String> targetTables = fks.stream().map(ForeignKey::targetTable).toList();
      assertTrue(targetTables.containsAll(List.of("customer_order", "product")));
    }

    @Test
    void itReturnsForeignKeys_customerOrder() throws SQLException {
      final Table order = Table.builder().schema("public").name("customer_order").build();
      final List<ForeignKey> fks = queryRunner.getForeignKeyInfo("public", order);

      assertEquals(2, fks.size());
      final List<String> targetTables = fks.stream().map(ForeignKey::targetTable).toList();
      assertTrue(targetTables.containsAll(List.of("app_user", "address")));
    }
  }
}
