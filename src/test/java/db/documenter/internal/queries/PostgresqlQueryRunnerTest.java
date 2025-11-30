package db.documenter.internal.queries;

import static org.junit.jupiter.api.Assertions.*;

import db.documenter.internal.models.db.*;
import db.documenter.internal.queries.impl.postgresql.PostgresqlQueryRunner;
import db.documenter.internal.queries.impl.postgresql.preparedstatements.PostgresqlPreparedStatementMapper;
import db.documenter.internal.queries.impl.postgresql.resultsets.PostgresqlResultSetMapper;
import db.documenter.internal.test.helpers.PostgresTestEnvironment;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.*;

class PostgresqlQueryRunnerTest {

  private static final PostgresTestEnvironment POSTGRES_TEST_ENVIRONMENT =
      new PostgresTestEnvironment();
  private PostgresqlQueryRunner postgresqlQueryRunner;
  private static Connection connection;

  @BeforeAll
  static void containerSetUp() throws SQLException, IOException {
    POSTGRES_TEST_ENVIRONMENT.startContainer();
    connection = POSTGRES_TEST_ENVIRONMENT.getConnection();
    POSTGRES_TEST_ENVIRONMENT.initialiseDatabase(
        connection, "/postgresql/single-schema/test-db.sql");
  }

  @BeforeEach
  void setUp() {
    postgresqlQueryRunner =
        new PostgresqlQueryRunner(
            new PostgresqlPreparedStatementMapper(), new PostgresqlResultSetMapper(), connection);
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
      final List<Table> tables = postgresqlQueryRunner.getTableInfo("public");
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
                  "audit_log",
                  "tag_log")));
    }
  }

  @Nested
  class ColumnInfoTests {

    @Nested
    class AppUserColumnTests {

      private Table appUser;

      @BeforeEach
      void setUp() {
        appUser =
            Table.builder().name("app_user").columns(List.of()).foreignKeys(List.of()).build();
      }

      @Test
      void itReturnsColumns() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", appUser);
        final List<String> names = columns.stream().map(Column::name).toList();

        final List<String> expectedColumnNames =
            List.of(
                "id",
                "username",
                "email",
                "display_name",
                "birth_date",
                "created_at",
                "profile",
                "is_active");

        assertTrue(names.containsAll(expectedColumnNames));
        assertEquals(expectedColumnNames.size(), names.size());
      }

      @Test
      void columnsHaveCorrectIsNullable() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", appUser);

        final var idColumn = columns.getFirst();
        final var userNameColumn = columns.get(1);
        final var emailColumn = columns.get(2);
        final var displayNameColumn = columns.get(3);
        final var birthDateColumn = columns.get(4);
        final var createdAtColumn = columns.get(5);
        final var profileColumn = columns.get(6);
        final var isActiveColumn = columns.getLast();

        assertEquals(8, columns.size());

        assertFalse(idColumn.isNullable());
        assertFalse(userNameColumn.isNullable());
        assertFalse(emailColumn.isNullable());
        assertTrue(displayNameColumn.isNullable());
        assertTrue(birthDateColumn.isNullable());
        assertFalse(createdAtColumn.isNullable());
        assertTrue(profileColumn.isNullable());
        assertFalse(isActiveColumn.isNullable());
      }

      @Test
      void columnsHaveCorrectDataType() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", appUser);

        final var idColumn = columns.getFirst();
        final var userNameColumn = columns.get(1);
        final var emailColumn = columns.get(2);
        final var displayNameColumn = columns.get(3);
        final var birthDateColumn = columns.get(4);
        final var createdAtColumn = columns.get(5);
        final var profileColumn = columns.get(6);
        final var isActiveColumn = columns.getLast();

        assertEquals(8, columns.size());

        assertEquals("uuid", idColumn.dataType());
        assertEquals("character varying", userNameColumn.dataType());
        assertEquals("character varying", emailColumn.dataType());
        assertEquals("character varying", displayNameColumn.dataType());
        assertEquals("date", birthDateColumn.dataType());
        assertEquals("timestamp with time zone", createdAtColumn.dataType());
        assertEquals("jsonb", profileColumn.dataType());
        assertEquals("boolean", isActiveColumn.dataType());
      }

      @Test
      void columnsHaveCorrectMaximumLength() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", appUser);

        final var idColumn = columns.getFirst();
        final var userNameColumn = columns.get(1);
        final var emailColumn = columns.get(2);
        final var displayNameColumn = columns.get(3);
        final var birthDateColumn = columns.get(4);
        final var createdAtColumn = columns.get(5);
        final var profileColumn = columns.get(6);
        final var isActiveColumn = columns.getLast();

        assertEquals(8, columns.size());

        assertEquals(0, idColumn.maximumLength());
        assertEquals(50, userNameColumn.maximumLength());
        assertEquals(255, emailColumn.maximumLength());
        assertEquals(100, displayNameColumn.maximumLength());
        assertEquals(0, birthDateColumn.maximumLength());
        assertEquals(0, createdAtColumn.maximumLength());
        assertEquals(0, profileColumn.maximumLength());
        assertEquals(0, isActiveColumn.maximumLength());
      }
    }

    @Nested
    class RoleColumnTests {

      private Table roleTable;

      @BeforeEach
      void setUp() {
        roleTable = Table.builder().name("role").columns(List.of()).foreignKeys(List.of()).build();
      }

      @Test
      void itReturnsColumns() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", roleTable);
        final List<String> names = columns.stream().map(Column::name).toList();

        final List<String> expectedColumnNames = List.of("id", "code", "description");

        assertTrue(names.containsAll(expectedColumnNames));
        assertEquals(expectedColumnNames.size(), names.size());
      }

      @Test
      void columnsHaveCorrectIsNullable() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", roleTable);

        final var idColumn = columns.getFirst();
        final var codeColumn = columns.get(1);
        final var descriptionColumn = columns.getLast();

        assertEquals(3, columns.size());

        assertFalse(idColumn.isNullable());
        assertFalse(codeColumn.isNullable());
        assertTrue(descriptionColumn.isNullable());
      }

      @Test
      void columnsHaveCorrectDataType() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", roleTable);

        final var idColumn = columns.getFirst();
        final var codeColumn = columns.get(1);
        final var descriptionColumn = columns.getLast();

        assertEquals(3, columns.size());

        assertEquals("smallint", idColumn.dataType());
        assertEquals("character varying", codeColumn.dataType());
        assertEquals("character varying", descriptionColumn.dataType());
      }

      @Test
      void columnsHaveCorrectMaximumLength() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", roleTable);

        final var idColumn = columns.getFirst();
        final var codeColumn = columns.get(1);
        final var descriptionColumn = columns.getLast();

        assertEquals(3, columns.size());

        assertEquals(0, idColumn.maximumLength());
        assertEquals(50, codeColumn.maximumLength());
        assertEquals(255, descriptionColumn.maximumLength());
      }
    }

    @Nested
    class UserRoleColumnTests {

      private Table userRole;

      @BeforeEach
      void setUp() {
        userRole =
            Table.builder().name("user_role").columns(List.of()).foreignKeys(List.of()).build();
      }

      @Test
      void itReturnsColumns() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", userRole);
        final List<String> names = columns.stream().map(Column::name).toList();

        final List<String> expectedColumnNames =
            List.of("user_id", "role_id", "assigned_at", "assigned_by");

        assertTrue(names.containsAll(expectedColumnNames));
        assertEquals(expectedColumnNames.size(), names.size());
      }

      @Test
      void columnsHaveCorrectIsNullable() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", userRole);

        final var userIdColumn = columns.getFirst();
        final var roleIdColumn = columns.get(1);
        final var assignedAtColumn = columns.get(2);
        final var assignedByColumn = columns.getLast();

        assertEquals(4, columns.size());

        assertFalse(userIdColumn.isNullable());
        assertFalse(roleIdColumn.isNullable());
        assertFalse(assignedAtColumn.isNullable());
        assertTrue(assignedByColumn.isNullable());
      }

      @Test
      void columnsHaveCorrectDataType() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", userRole);

        final var userIdColumn = columns.getFirst();
        final var roleIdColumn = columns.get(1);
        final var assignedAtColumn = columns.get(2);
        final var assignedByColumn = columns.getLast();

        assertEquals(4, columns.size());

        assertEquals("uuid", userIdColumn.dataType());
        assertEquals("smallint", roleIdColumn.dataType());
        assertEquals("timestamp with time zone", assignedAtColumn.dataType());
        assertEquals("uuid", assignedByColumn.dataType());
      }

      @Test
      void columnsHaveCorrectMaximumLength() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", userRole);

        final var userIdColumn = columns.getFirst();
        final var roleIdColumn = columns.get(1);
        final var assignedAtColumn = columns.get(2);
        final var assignedByColumn = columns.getLast();

        assertEquals(4, columns.size());

        assertEquals(0, userIdColumn.maximumLength());
        assertEquals(0, roleIdColumn.maximumLength());
        assertEquals(0, assignedAtColumn.maximumLength());
        assertEquals(0, assignedByColumn.maximumLength());
      }
    }

    @Nested
    class AddressColumnTests {

      private Table address;

      @BeforeEach
      void setUp() {
        address = Table.builder().name("address").columns(List.of()).foreignKeys(List.of()).build();
      }

      @Test
      void itReturnsColumns() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", address);
        final List<String> names = columns.stream().map(Column::name).toList();

        final List<String> expectedColumnNames =
            List.of(
                "id",
                "user_id",
                "line1",
                "line2",
                "city",
                "state",
                "postal_code",
                "country",
                "created_at");

        assertTrue(names.containsAll(expectedColumnNames));
        assertEquals(expectedColumnNames.size(), names.size());
      }

      @Test
      void columnsHaveCorrectIsNullable() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", address);

        final var idColumn = columns.getFirst();
        final var userIdColumn = columns.get(1);
        final var line1Column = columns.get(2);
        final var line2Column = columns.get(3);
        final var cityColumn = columns.get(4);
        final var stateColumn = columns.get(5);
        final var postalCodeColumn = columns.get(6);
        final var countryColumn = columns.get(7);
        final var createdAtColumn = columns.getLast();

        assertEquals(9, columns.size());

        assertFalse(idColumn.isNullable());
        assertFalse(userIdColumn.isNullable());
        assertFalse(line1Column.isNullable());
        assertTrue(line2Column.isNullable());
        assertFalse(cityColumn.isNullable());
        assertTrue(stateColumn.isNullable());
        assertTrue(postalCodeColumn.isNullable());
        assertFalse(countryColumn.isNullable());
        assertFalse(createdAtColumn.isNullable());
      }

      @Test
      void columnsHaveCorrectDataType() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", address);

        final var idColumn = columns.getFirst();
        final var userIdColumn = columns.get(1);
        final var line1Column = columns.get(2);
        final var line2Column = columns.get(3);
        final var cityColumn = columns.get(4);
        final var stateColumn = columns.get(5);
        final var postalCodeColumn = columns.get(6);
        final var countryColumn = columns.get(7);
        final var createdAtColumn = columns.getLast();

        assertEquals(9, columns.size());

        assertEquals("bigint", idColumn.dataType());
        assertEquals("uuid", userIdColumn.dataType());
        assertEquals("character varying", line1Column.dataType());
        assertEquals("character varying", line2Column.dataType());
        assertEquals("character varying", cityColumn.dataType());
        assertEquals("character varying", stateColumn.dataType());
        assertEquals("character varying", postalCodeColumn.dataType());
        assertEquals("character", countryColumn.dataType());
        assertEquals("timestamp with time zone", createdAtColumn.dataType());
      }

      @Test
      void columnsHaveCorrectMaximumLength() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", address);

        final var idColumn = columns.getFirst();
        final var userIdColumn = columns.get(1);
        final var line1Column = columns.get(2);
        final var line2Column = columns.get(3);
        final var cityColumn = columns.get(4);
        final var stateColumn = columns.get(5);
        final var postalCodeColumn = columns.get(6);
        final var countryColumn = columns.get(7);
        final var createdAtColumn = columns.getLast();

        assertEquals(9, columns.size());

        assertEquals(0, idColumn.maximumLength());
        assertEquals(0, userIdColumn.maximumLength());
        assertEquals(200, line1Column.maximumLength());
        assertEquals(200, line2Column.maximumLength());
        assertEquals(100, cityColumn.maximumLength());
        assertEquals(100, stateColumn.maximumLength());
        assertEquals(20, postalCodeColumn.maximumLength());
        assertEquals(2, countryColumn.maximumLength());
        assertEquals(0, createdAtColumn.maximumLength());
      }
    }

    @Nested
    class ProductColumnTests {

      private Table product;

      @BeforeEach
      void setUp() {
        product = Table.builder().name("product").columns(List.of()).foreignKeys(List.of()).build();
      }

      @Test
      void itReturnsColumns() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", product);
        final List<String> names = columns.stream().map(Column::name).toList();

        final List<String> expectedColumnNames =
            List.of(
                "id", "sku", "name", "description", "price", "attributes", "created_at", "stock");

        assertTrue(names.containsAll(expectedColumnNames));
        assertEquals(expectedColumnNames.size(), names.size());
      }

      @Test
      void columnsHaveCorrectIsNullable() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", product);

        final var idColumn = columns.getFirst();
        final var skuColumn = columns.get(1);
        final var nameColumn = columns.get(2);
        final var descriptionColumn = columns.get(3);
        final var priceColumn = columns.get(4);
        final var attributesColumn = columns.get(5);
        final var createdAtColumn = columns.get(6);
        final var stockColumn = columns.getLast();

        assertEquals(8, columns.size());

        assertFalse(idColumn.isNullable());
        assertFalse(skuColumn.isNullable());
        assertFalse(nameColumn.isNullable());
        assertTrue(descriptionColumn.isNullable());
        assertFalse(priceColumn.isNullable());
        assertTrue(attributesColumn.isNullable());
        assertFalse(createdAtColumn.isNullable());
        assertFalse(stockColumn.isNullable());
      }

      @Test
      void columnsHaveCorrectDataType() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", product);

        final var idColumn = columns.getFirst();
        final var skuColumn = columns.get(1);
        final var nameColumn = columns.get(2);
        final var descriptionColumn = columns.get(3);
        final var priceColumn = columns.get(4);
        final var attributesColumn = columns.get(5);
        final var createdAtColumn = columns.get(6);
        final var stockColumn = columns.getLast();

        assertEquals(8, columns.size());

        assertEquals("bigint", idColumn.dataType());
        assertEquals("character varying", skuColumn.dataType());
        assertEquals("character varying", nameColumn.dataType());
        assertEquals("text", descriptionColumn.dataType());
        assertEquals("numeric", priceColumn.dataType());
        assertEquals("jsonb", attributesColumn.dataType());
        assertEquals("timestamp without time zone", createdAtColumn.dataType());
        assertEquals("integer", stockColumn.dataType());
      }

      @Test
      void columnsHaveCorrectMaximumLength() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", product);

        final var idColumn = columns.getFirst();
        final var skuColumn = columns.get(1);
        final var nameColumn = columns.get(2);
        final var descriptionColumn = columns.get(3);
        final var priceColumn = columns.get(4);
        final var attributesColumn = columns.get(5);
        final var createdAtColumn = columns.get(6);
        final var stockColumn = columns.getLast();

        assertEquals(8, columns.size());

        assertEquals(0, idColumn.maximumLength());
        assertEquals(30, skuColumn.maximumLength());
        assertEquals(200, nameColumn.maximumLength());
        assertEquals(0, descriptionColumn.maximumLength());
        assertEquals(0, priceColumn.maximumLength());
        assertEquals(0, attributesColumn.maximumLength());
        assertEquals(0, createdAtColumn.maximumLength());
        assertEquals(0, stockColumn.maximumLength());
      }
    }

    @Nested
    class CategoryColumnTests {

      private Table category;

      @BeforeEach
      void setUp() {
        category =
            Table.builder().name("category").columns(List.of()).foreignKeys(List.of()).build();
      }

      @Test
      void itReturnsColumns() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", category);
        final List<String> names = columns.stream().map(Column::name).toList();

        final List<String> expectedColumnNames = List.of("id", "slug", "title", "metadata");

        assertTrue(names.containsAll(expectedColumnNames));
        assertEquals(expectedColumnNames.size(), names.size());
      }

      @Test
      void columnsHaveCorrectIsNullable() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", category);

        final var idColumn = columns.getFirst();
        final var slugColumn = columns.get(1);
        final var titleColumn = columns.get(2);
        final var metadataColumn = columns.getLast();

        assertEquals(4, columns.size());

        assertFalse(idColumn.isNullable());
        assertFalse(slugColumn.isNullable());
        assertFalse(titleColumn.isNullable());
        assertTrue(metadataColumn.isNullable());
      }

      @Test
      void columnsHaveCorrectDataType() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", category);

        final var idColumn = columns.getFirst();
        final var slugColumn = columns.get(1);
        final var titleColumn = columns.get(2);
        final var metadataColumn = columns.getLast();

        assertEquals(4, columns.size());

        assertEquals("smallint", idColumn.dataType());
        assertEquals("character varying", slugColumn.dataType());
        assertEquals("character varying", titleColumn.dataType());
        assertEquals("jsonb", metadataColumn.dataType());
      }

      @Test
      void columnsHaveCorrectMaximumLength() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", category);

        final var idColumn = columns.getFirst();
        final var slugColumn = columns.get(1);
        final var titleColumn = columns.get(2);
        final var metadataColumn = columns.getLast();

        assertEquals(4, columns.size());

        assertEquals(0, idColumn.maximumLength());
        assertEquals(80, slugColumn.maximumLength());
        assertEquals(120, titleColumn.maximumLength());
        assertEquals(0, metadataColumn.maximumLength());
      }
    }

    @Nested
    class ProductCategoryColumnTests {

      private Table productCategory;

      @BeforeEach
      void setUp() {
        productCategory =
            Table.builder()
                .name("product_category")
                .columns(List.of())
                .foreignKeys(List.of())
                .build();
      }

      @Test
      void itReturnsColumns() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", productCategory);
        final List<String> names = columns.stream().map(Column::name).toList();

        final List<String> expectedColumnNames =
            List.of("product_id", "category_id", "assigned_at");

        assertTrue(names.containsAll(expectedColumnNames));
        assertEquals(expectedColumnNames.size(), names.size());
      }

      @Test
      void columnsHaveCorrectIsNullable() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", productCategory);

        final var productIdColumn = columns.getFirst();
        final var categoryIdColumn = columns.get(1);
        final var assignedAtColumn = columns.getLast();

        assertEquals(3, columns.size());

        assertFalse(productIdColumn.isNullable());
        assertFalse(categoryIdColumn.isNullable());
        assertFalse(assignedAtColumn.isNullable());
      }

      @Test
      void columnsHaveCorrectDataType() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", productCategory);

        final var productIdColumn = columns.getFirst();
        final var categoryIdColumn = columns.get(1);
        final var assignedAtColumn = columns.getLast();

        assertEquals(3, columns.size());

        assertEquals("bigint", productIdColumn.dataType());
        assertEquals("smallint", categoryIdColumn.dataType());
        assertEquals("timestamp with time zone", assignedAtColumn.dataType());
      }

      @Test
      void columnsHaveCorrectMaximumLength() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", productCategory);

        final var productIdColumn = columns.getFirst();
        final var categoryIdColumn = columns.get(1);
        final var assignedAtColumn = columns.getLast();

        assertEquals(3, columns.size());

        assertEquals(0, productIdColumn.maximumLength());
        assertEquals(0, categoryIdColumn.maximumLength());
        assertEquals(0, assignedAtColumn.maximumLength());
      }
    }

    @Nested
    class CustomerOrderColumnTests {

      private Table customerOrder;

      @BeforeEach
      void setUp() {
        customerOrder =
            Table.builder()
                .name("customer_order")
                .columns(List.of())
                .foreignKeys(List.of())
                .build();
      }

      @Test
      void itReturnsColumns() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", customerOrder);
        final List<String> names = columns.stream().map(Column::name).toList();

        final List<String> expectedColumnNames =
            List.of(
                "id",
                "user_id",
                "order_number",
                "order_date",
                "ship_date",
                "shipping_address_id",
                "status",
                "total",
                "metadata");

        assertTrue(names.containsAll(expectedColumnNames));
        assertEquals(expectedColumnNames.size(), names.size());
      }

      @Test
      void columnsHaveCorrectIsNullable() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", customerOrder);

        final var idColumn = columns.getFirst();
        final var userIdColumn = columns.get(1);
        final var orderNumberColumn = columns.get(2);
        final var orderDateColumn = columns.get(3);
        final var shipDateColumn = columns.get(4);
        final var shippingAddressIdColumn = columns.get(5);
        final var statusColumn = columns.get(6);
        final var totalColumn = columns.get(7);
        final var metadataColumn = columns.getLast();

        assertEquals(9, columns.size());

        assertFalse(idColumn.isNullable());
        assertTrue(userIdColumn.isNullable());
        assertFalse(orderNumberColumn.isNullable());
        assertFalse(orderDateColumn.isNullable());
        assertTrue(shipDateColumn.isNullable());
        assertTrue(shippingAddressIdColumn.isNullable());
        assertFalse(statusColumn.isNullable());
        assertFalse(totalColumn.isNullable());
        assertTrue(metadataColumn.isNullable());
      }

      @Test
      void columnsHaveCorrectDataType() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", customerOrder);

        final var idColumn = columns.getFirst();
        final var userIdColumn = columns.get(1);
        final var orderNumberColumn = columns.get(2);
        final var orderDateColumn = columns.get(3);
        final var shipDateColumn = columns.get(4);
        final var shippingAddressIdColumn = columns.get(5);
        final var statusColumn = columns.get(6);
        final var totalColumn = columns.get(7);
        final var metadataColumn = columns.getLast();

        assertEquals(9, columns.size());

        assertEquals("bigint", idColumn.dataType());
        assertEquals("uuid", userIdColumn.dataType());
        assertEquals("character varying", orderNumberColumn.dataType());
        assertEquals("timestamp with time zone", orderDateColumn.dataType());
        assertEquals("timestamp without time zone", shipDateColumn.dataType());
        assertEquals("bigint", shippingAddressIdColumn.dataType());
        assertEquals("USER-DEFINED", statusColumn.dataType());
        assertEquals("numeric", totalColumn.dataType());
        assertEquals("jsonb", metadataColumn.dataType());
      }

      @Test
      void columnsHaveCorrectMaximumLength() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", customerOrder);

        final var idColumn = columns.getFirst();
        final var userIdColumn = columns.get(1);
        final var orderNumberColumn = columns.get(2);
        final var orderDateColumn = columns.get(3);
        final var shipDateColumn = columns.get(4);
        final var shippingAddressIdColumn = columns.get(5);
        final var statusColumn = columns.get(6);
        final var totalColumn = columns.get(7);
        final var metadataColumn = columns.getLast();

        assertEquals(9, columns.size());

        assertEquals(0, idColumn.maximumLength());
        assertEquals(0, userIdColumn.maximumLength());
        assertEquals(30, orderNumberColumn.maximumLength());
        assertEquals(0, orderDateColumn.maximumLength());
        assertEquals(0, shipDateColumn.maximumLength());
        assertEquals(0, shippingAddressIdColumn.maximumLength());
        assertEquals(0, statusColumn.maximumLength());
        assertEquals(0, totalColumn.maximumLength());
        assertEquals(0, metadataColumn.maximumLength());
      }
    }

    @Nested
    class OrderItemColumnTests {

      private Table orderItem;

      @BeforeEach
      void setUp() {
        orderItem =
            Table.builder().name("order_item").columns(List.of()).foreignKeys(List.of()).build();
      }

      @Test
      void itReturnsColumns() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", orderItem);
        final List<String> names = columns.stream().map(Column::name).toList();

        final List<String> expectedColumnNames =
            List.of(
                "id",
                "order_id",
                "product_id",
                "product_snapshot",
                "unit_price",
                "quantity",
                "line_total");

        assertTrue(names.containsAll(expectedColumnNames));
        assertEquals(expectedColumnNames.size(), names.size());
      }

      @Test
      void columnsHaveCorrectIsNullable() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", orderItem);

        final var idColumn = columns.getFirst();
        final var orderIdColumn = columns.get(1);
        final var productIdColumn = columns.get(2);
        final var productSnapshotColumn = columns.get(3);
        final var unitPriceColumn = columns.get(4);
        final var quantityColumn = columns.get(5);
        final var lineTotalColumn = columns.getLast();

        assertEquals(7, columns.size());

        assertFalse(idColumn.isNullable());
        assertFalse(orderIdColumn.isNullable());
        assertFalse(productIdColumn.isNullable());
        assertFalse(productSnapshotColumn.isNullable());
        assertFalse(unitPriceColumn.isNullable());
        assertFalse(quantityColumn.isNullable());
        assertFalse(lineTotalColumn.isNullable());
      }

      @Test
      void columnsHaveCorrectDataType() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", orderItem);

        final var idColumn = columns.getFirst();
        final var orderIdColumn = columns.get(1);
        final var productIdColumn = columns.get(2);
        final var productSnapshotColumn = columns.get(3);
        final var unitPriceColumn = columns.get(4);
        final var quantityColumn = columns.get(5);
        final var lineTotalColumn = columns.getLast();

        assertEquals(7, columns.size());

        assertEquals("bigint", idColumn.dataType());
        assertEquals("bigint", orderIdColumn.dataType());
        assertEquals("bigint", productIdColumn.dataType());
        assertEquals("jsonb", productSnapshotColumn.dataType());
        assertEquals("numeric", unitPriceColumn.dataType());
        assertEquals("integer", quantityColumn.dataType());
        assertEquals("numeric", lineTotalColumn.dataType());
      }

      @Test
      void columnsHaveCorrectMaximumLength() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", orderItem);

        final var idColumn = columns.getFirst();
        final var orderIdColumn = columns.get(1);
        final var productIdColumn = columns.get(2);
        final var productSnapshotColumn = columns.get(3);
        final var unitPriceColumn = columns.get(4);
        final var quantityColumn = columns.get(5);
        final var lineTotalColumn = columns.getLast();

        assertEquals(7, columns.size());

        assertEquals(0, idColumn.maximumLength());
        assertEquals(0, orderIdColumn.maximumLength());
        assertEquals(0, productIdColumn.maximumLength());
        assertEquals(0, productSnapshotColumn.maximumLength());
        assertEquals(0, unitPriceColumn.maximumLength());
        assertEquals(0, quantityColumn.maximumLength());
        assertEquals(0, lineTotalColumn.maximumLength());
      }
    }

    @Nested
    class PaymentColumnTests {

      private Table payment;

      @BeforeEach
      void setUp() {
        payment = Table.builder().name("payment").columns(List.of()).foreignKeys(List.of()).build();
      }

      @Test
      void itReturnsColumns() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", payment);
        final List<String> names = columns.stream().map(Column::name).toList();

        final List<String> expectedColumnNames =
            List.of(
                "id",
                "order_id",
                "paid_at",
                "amount",
                "method",
                "provider_transaction_id",
                "raw_response");

        assertTrue(names.containsAll(expectedColumnNames));
        assertEquals(expectedColumnNames.size(), names.size());
      }

      @Test
      void columnsHaveCorrectIsNullable() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", payment);

        final var idColumn = columns.getFirst();
        final var orderIdColumn = columns.get(1);
        final var paidAtColumn = columns.get(2);
        final var amountColumn = columns.get(3);
        final var methodColumn = columns.get(4);
        final var providerTransactionIdColumn = columns.get(5);
        final var rawResponseColumn = columns.getLast();

        assertEquals(7, columns.size());

        assertFalse(idColumn.isNullable());
        assertFalse(orderIdColumn.isNullable());
        assertFalse(paidAtColumn.isNullable());
        assertFalse(amountColumn.isNullable());
        assertFalse(methodColumn.isNullable());
        assertTrue(providerTransactionIdColumn.isNullable());
        assertTrue(rawResponseColumn.isNullable());
      }

      @Test
      void columnsHaveCorrectDataType() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", payment);

        final var idColumn = columns.getFirst();
        final var orderIdColumn = columns.get(1);
        final var paidAtColumn = columns.get(2);
        final var amountColumn = columns.get(3);
        final var methodColumn = columns.get(4);
        final var providerTransactionIdColumn = columns.get(5);
        final var rawResponseColumn = columns.getLast();

        assertEquals(7, columns.size());

        assertEquals("uuid", idColumn.dataType());
        assertEquals("bigint", orderIdColumn.dataType());
        assertEquals("timestamp with time zone", paidAtColumn.dataType());
        assertEquals("numeric", amountColumn.dataType());
        assertEquals("character varying", methodColumn.dataType());
        assertEquals("character varying", providerTransactionIdColumn.dataType());
        assertEquals("jsonb", rawResponseColumn.dataType());
      }

      @Test
      void columnsHaveCorrectMaximumLength() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", payment);

        final var idColumn = columns.getFirst();
        final var orderIdColumn = columns.get(1);
        final var paidAtColumn = columns.get(2);
        final var amountColumn = columns.get(3);
        final var methodColumn = columns.get(4);
        final var providerTransactionIdColumn = columns.get(5);
        final var rawResponseColumn = columns.getLast();

        assertEquals(7, columns.size());

        assertEquals(0, idColumn.maximumLength());
        assertEquals(0, orderIdColumn.maximumLength());
        assertEquals(0, paidAtColumn.maximumLength());
        assertEquals(0, amountColumn.maximumLength());
        assertEquals(50, methodColumn.maximumLength());
        assertEquals(255, providerTransactionIdColumn.maximumLength());
        assertEquals(0, rawResponseColumn.maximumLength());
      }
    }

    @Nested
    class AuditLogColumnTests {

      private Table auditLog;

      @BeforeEach
      void setUp() {
        auditLog =
            Table.builder().name("audit_log").columns(List.of()).foreignKeys(List.of()).build();
      }

      @Test
      void itReturnsColumns() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", auditLog);
        final List<String> names = columns.stream().map(Column::name).toList();

        final List<String> expectedColumnNames =
            List.of(
                "id",
                "entity_type",
                "entity_id",
                "action",
                "performed_by",
                "performed_at",
                "details");

        assertTrue(names.containsAll(expectedColumnNames));
        assertEquals(expectedColumnNames.size(), names.size());
      }

      @Test
      void columnsHaveCorrectIsNullable() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", auditLog);

        final var idColumn = columns.getFirst();
        final var entityTypeColumn = columns.get(1);
        final var entityIdColumn = columns.get(2);
        final var actionColumn = columns.get(3);
        final var performedByColumn = columns.get(4);
        final var performedAtColumn = columns.get(5);
        final var detailsColumn = columns.getLast();

        assertEquals(7, columns.size());

        assertFalse(idColumn.isNullable());
        assertFalse(entityTypeColumn.isNullable());
        assertFalse(entityIdColumn.isNullable());
        assertFalse(actionColumn.isNullable());
        assertTrue(performedByColumn.isNullable());
        assertFalse(performedAtColumn.isNullable());
        assertTrue(detailsColumn.isNullable());
      }

      @Test
      void columnsHaveCorrectDataType() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", auditLog);

        final var idColumn = columns.getFirst();
        final var entityTypeColumn = columns.get(1);
        final var entityIdColumn = columns.get(2);
        final var actionColumn = columns.get(3);
        final var performedByColumn = columns.get(4);
        final var performedAtColumn = columns.get(5);
        final var detailsColumn = columns.getLast();

        assertEquals(7, columns.size());

        assertEquals("bigint", idColumn.dataType());
        assertEquals("character varying", entityTypeColumn.dataType());
        assertEquals("text", entityIdColumn.dataType());
        assertEquals("character varying", actionColumn.dataType());
        assertEquals("uuid", performedByColumn.dataType());
        assertEquals("timestamp with time zone", performedAtColumn.dataType());
        assertEquals("jsonb", detailsColumn.dataType());
      }

      @Test
      void columnsHaveCorrectMaximumLength() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", auditLog);

        final var idColumn = columns.getFirst();
        final var entityTypeColumn = columns.get(1);
        final var entityIdColumn = columns.get(2);
        final var actionColumn = columns.get(3);
        final var performedByColumn = columns.get(4);
        final var performedAtColumn = columns.get(5);
        final var detailsColumn = columns.getLast();

        assertEquals(7, columns.size());

        assertEquals(0, idColumn.maximumLength());
        assertEquals(100, entityTypeColumn.maximumLength());
        assertEquals(0, entityIdColumn.maximumLength());
        assertEquals(50, actionColumn.maximumLength());
        assertEquals(0, performedByColumn.maximumLength());
        assertEquals(0, performedAtColumn.maximumLength());
        assertEquals(0, detailsColumn.maximumLength());
      }
    }

    @Nested
    class TagLogColumnTests {

      private Table tagLog;

      @BeforeEach
      void setUp() {
        tagLog = Table.builder().name("tag_log").columns(List.of()).foreignKeys(List.of()).build();
      }

      @Test
      void itReturnsColumns() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", tagLog);
        final List<String> names = columns.stream().map(Column::name).toList();

        final List<String> expectedColumnNames = List.of("tag_id", "logged_at");

        assertTrue(names.containsAll(expectedColumnNames));
        assertEquals(expectedColumnNames.size(), names.size());
      }

      @Test
      void columnsHaveCorrectIsNullable() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", tagLog);

        final var tagIdColumn = columns.getFirst();
        final var loggedAtColumn = columns.getLast();

        assertEquals(2, columns.size());

        assertFalse(tagIdColumn.isNullable());
        assertFalse(loggedAtColumn.isNullable());
      }

      @Test
      void columnsHaveCorrectDataType() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", tagLog);

        final var tagIdColumn = columns.getFirst();
        final var loggedAtColumn = columns.getLast();

        assertEquals(2, columns.size());

        assertEquals("character varying", tagIdColumn.dataType());
        assertEquals("timestamp with time zone", loggedAtColumn.dataType());
      }

      @Test
      void columnsHaveCorrectMaximumLength() throws SQLException {
        final List<Column> columns = postgresqlQueryRunner.getColumnInfo("public", tagLog);

        final var tagIdColumn = columns.getFirst();
        final var loggedAtColumn = columns.getLast();

        assertEquals(2, columns.size());

        assertEquals(50, tagIdColumn.maximumLength());
        assertEquals(0, loggedAtColumn.maximumLength());
      }
    }
  }

  @Nested
  class PrimaryKeyTests {

    @Nested
    class AppUserPrimaryKeyTests {

      private Table appUser;

      @BeforeEach
      void setUp() {
        appUser =
            Table.builder().name("app_user").columns(List.of()).foreignKeys(List.of()).build();
      }

      @Test
      void itHasTheCorrectConstraintName() throws SQLException {
        final PrimaryKey primaryKey = postgresqlQueryRunner.getPrimaryKeyInfo("public", appUser);

        assertEquals("app_user_pkey", primaryKey.constraintName());
      }

      @Test
      void itHasTheCorrectColumnNames() throws SQLException {
        final PrimaryKey primaryKey = postgresqlQueryRunner.getPrimaryKeyInfo("public", appUser);

        assertEquals(1, primaryKey.columnNames().size());
        assertEquals("id", primaryKey.columnNames().getFirst());
      }
    }

    @Nested
    class RolePrimaryKeyTests {

      private Table role;

      @BeforeEach
      void setUp() {
        role = Table.builder().name("role").columns(List.of()).foreignKeys(List.of()).build();
      }

      @Test
      void itHasTheCorrectConstraintName() throws SQLException {
        final PrimaryKey primaryKey = postgresqlQueryRunner.getPrimaryKeyInfo("public", role);

        assertEquals("role_pkey", primaryKey.constraintName());
      }

      @Test
      void itHasTheCorrectColumnNames() throws SQLException {
        final PrimaryKey primaryKey = postgresqlQueryRunner.getPrimaryKeyInfo("public", role);

        assertEquals(1, primaryKey.columnNames().size());
        assertEquals("id", primaryKey.columnNames().getFirst());
      }
    }

    @Nested
    class UserRolePrimaryKeyTests {

      private Table userRole;

      @BeforeEach
      void setUp() {
        userRole =
            Table.builder().name("user_role").columns(List.of()).foreignKeys(List.of()).build();
      }

      @Test
      void itHasTheCorrectConstraintName() throws SQLException {
        final PrimaryKey primaryKey = postgresqlQueryRunner.getPrimaryKeyInfo("public", userRole);

        assertEquals("user_role_pkey", primaryKey.constraintName());
      }

      @Test
      void itHasTheCorrectColumnNames() throws SQLException {
        final PrimaryKey primaryKey = postgresqlQueryRunner.getPrimaryKeyInfo("public", userRole);

        assertEquals(2, primaryKey.columnNames().size());
        assertEquals("user_id", primaryKey.columnNames().getFirst());
        assertEquals("role_id", primaryKey.columnNames().get(1));
      }
    }

    @Nested
    class AddressPrimaryKeyTests {

      private Table address;

      @BeforeEach
      void setUp() {
        address = Table.builder().name("address").columns(List.of()).foreignKeys(List.of()).build();
      }

      @Test
      void itHasTheCorrectConstraintName() throws SQLException {
        final PrimaryKey primaryKey = postgresqlQueryRunner.getPrimaryKeyInfo("public", address);

        assertEquals("address_pkey", primaryKey.constraintName());
      }

      @Test
      void itHasTheCorrectColumnNames() throws SQLException {
        final PrimaryKey primaryKey = postgresqlQueryRunner.getPrimaryKeyInfo("public", address);

        assertEquals(1, primaryKey.columnNames().size());
        assertEquals("id", primaryKey.columnNames().getFirst());
      }
    }

    @Nested
    class ProductPrimaryKeyTests {

      private Table product;

      @BeforeEach
      void setUp() {
        product = Table.builder().name("product").columns(List.of()).foreignKeys(List.of()).build();
      }

      @Test
      void itHasTheCorrectConstraintName() throws SQLException {
        final PrimaryKey primaryKey = postgresqlQueryRunner.getPrimaryKeyInfo("public", product);

        assertEquals("product_pkey", primaryKey.constraintName());
      }

      @Test
      void itHasTheCorrectColumnNames() throws SQLException {
        final PrimaryKey primaryKey = postgresqlQueryRunner.getPrimaryKeyInfo("public", product);

        assertEquals(1, primaryKey.columnNames().size());
        assertEquals("id", primaryKey.columnNames().getFirst());
      }
    }

    @Nested
    class CategoryPrimaryKeyTests {

      private Table category;

      @BeforeEach
      void setUp() {
        category =
            Table.builder().name("category").columns(List.of()).foreignKeys(List.of()).build();
      }

      @Test
      void itHasTheCorrectConstraintName() throws SQLException {
        final PrimaryKey primaryKey = postgresqlQueryRunner.getPrimaryKeyInfo("public", category);

        assertEquals("category_pkey", primaryKey.constraintName());
      }

      @Test
      void itHasTheCorrectColumnNames() throws SQLException {
        final PrimaryKey primaryKey = postgresqlQueryRunner.getPrimaryKeyInfo("public", category);

        assertEquals(1, primaryKey.columnNames().size());
        assertEquals("id", primaryKey.columnNames().getFirst());
      }
    }

    @Nested
    class ProductCategoryPrimaryKeyTests {

      private Table productCategory;

      @BeforeEach
      void setUp() {
        productCategory =
            Table.builder()
                .name("product_category")
                .columns(List.of())
                .foreignKeys(List.of())
                .build();
      }

      @Test
      void itHasTheCorrectConstraintName() throws SQLException {
        final PrimaryKey primaryKey =
            postgresqlQueryRunner.getPrimaryKeyInfo("public", productCategory);

        assertEquals("product_category_pkey", primaryKey.constraintName());
      }

      @Test
      void itHasTheCorrectColumnNames() throws SQLException {
        final PrimaryKey primaryKey =
            postgresqlQueryRunner.getPrimaryKeyInfo("public", productCategory);

        assertEquals(2, primaryKey.columnNames().size());
        assertEquals("product_id", primaryKey.columnNames().getFirst());
        assertEquals("category_id", primaryKey.columnNames().get(1));
      }
    }

    @Nested
    class CustomerOrderPrimaryKeyTests {

      private Table customerOrder;

      @BeforeEach
      void setUp() {
        customerOrder =
            Table.builder()
                .name("customer_order")
                .columns(List.of())
                .foreignKeys(List.of())
                .build();
      }

      @Test
      void itHasTheCorrectConstraintName() throws SQLException {
        final PrimaryKey primaryKey =
            postgresqlQueryRunner.getPrimaryKeyInfo("public", customerOrder);

        assertEquals("customer_order_pkey", primaryKey.constraintName());
      }

      @Test
      void itHasTheCorrectColumnNames() throws SQLException {
        final PrimaryKey primaryKey =
            postgresqlQueryRunner.getPrimaryKeyInfo("public", customerOrder);

        assertEquals(1, primaryKey.columnNames().size());
        assertEquals("id", primaryKey.columnNames().getFirst());
      }
    }

    @Nested
    class OrderItemPrimaryKeyTests {

      private Table orderItem;

      @BeforeEach
      void setUp() {
        orderItem =
            Table.builder().name("order_item").columns(List.of()).foreignKeys(List.of()).build();
      }

      @Test
      void itHasTheCorrectConstraintName() throws SQLException {
        final PrimaryKey primaryKey = postgresqlQueryRunner.getPrimaryKeyInfo("public", orderItem);

        assertEquals("order_item_pkey", primaryKey.constraintName());
      }

      @Test
      void itHasTheCorrectColumnNames() throws SQLException {
        final PrimaryKey primaryKey = postgresqlQueryRunner.getPrimaryKeyInfo("public", orderItem);

        assertEquals(1, primaryKey.columnNames().size());
        assertEquals("id", primaryKey.columnNames().getFirst());
      }
    }

    @Nested
    class PaymentPrimaryKeyTests {

      private Table payment;

      @BeforeEach
      void setUp() {
        payment = Table.builder().name("payment").columns(List.of()).foreignKeys(List.of()).build();
      }

      @Test
      void itHasTheCorrectConstraintName() throws SQLException {
        final PrimaryKey primaryKey = postgresqlQueryRunner.getPrimaryKeyInfo("public", payment);

        assertEquals("payment_pkey", primaryKey.constraintName());
      }

      @Test
      void itHasTheCorrectColumnNames() throws SQLException {
        final PrimaryKey primaryKey = postgresqlQueryRunner.getPrimaryKeyInfo("public", payment);

        assertEquals(1, primaryKey.columnNames().size());
        assertEquals("id", primaryKey.columnNames().getFirst());
      }
    }

    @Nested
    class AuditLogPrimaryKeyTests {

      private Table auditLog;

      @BeforeEach
      void setUp() {
        auditLog =
            Table.builder().name("audit_log").columns(List.of()).foreignKeys(List.of()).build();
      }

      @Test
      void itHasTheCorrectConstraintName() throws SQLException {
        final PrimaryKey primaryKey = postgresqlQueryRunner.getPrimaryKeyInfo("public", auditLog);

        assertEquals("audit_log_pkey", primaryKey.constraintName());
      }

      @Test
      void itHasTheCorrectColumnNames() throws SQLException {
        final PrimaryKey primaryKey = postgresqlQueryRunner.getPrimaryKeyInfo("public", auditLog);

        assertEquals(1, primaryKey.columnNames().size());
        assertEquals("id", primaryKey.columnNames().getFirst());
      }
    }

    @Nested
    class TagLogPrimaryKeyTests {

      private Table tagLog;

      @BeforeEach
      void setUp() {
        tagLog = Table.builder().name("tag_log").columns(List.of()).foreignKeys(List.of()).build();
      }

      @Test
      void itHasNoPrimaryKey() throws SQLException {
        final PrimaryKey primaryKey = postgresqlQueryRunner.getPrimaryKeyInfo("public", tagLog);
        assertNull(primaryKey);
      }
    }
  }

  @Nested
  class ForeignKeyTests {

    @Nested
    class AppUserForeignKeyTests {

      private Table appUser;

      @BeforeEach
      void setUp() {
        appUser =
            Table.builder().name("app_user").columns(List.of()).foreignKeys(List.of()).build();
      }

      @Test
      void appUserForeignKeyIsEmpty() throws SQLException {
        final var appUserForeignKey = postgresqlQueryRunner.getForeignKeyInfo("public", appUser);
        assertNotNull(appUserForeignKey);
        assertTrue(appUserForeignKey.isEmpty());
      }
    }
  }

  @Nested
  class AddressForeignKeyTests {
    private Table address;

    @BeforeEach
    void setUp() {
      address = Table.builder().name("address").columns(List.of()).foreignKeys(List.of()).build();
    }

    @Test
    void itHasForeignKeyToAppUser() throws SQLException {
      final List<ForeignKey> fks = postgresqlQueryRunner.getForeignKeyInfo("public", address);

      assertEquals(1, fks.size());

      final ForeignKey fk = fks.getFirst();
      assertEquals("address_user_id_fkey", fk.name());
      assertEquals("user_id", fk.sourceColumn());
      assertEquals("app_user", fk.targetTable());
      assertEquals("id", fk.targetColumn());
    }
  }

  @Nested
  class ProductForeignKeyTests {
    private Table product;

    @BeforeEach
    void setUp() {
      product = Table.builder().name("product").columns(List.of()).foreignKeys(List.of()).build();
    }

    @Test
    void itHasNoForeignKeys() throws SQLException {
      final List<ForeignKey> foreignKeys =
          postgresqlQueryRunner.getForeignKeyInfo("public", product);
      assertTrue(foreignKeys.isEmpty());
    }
  }

  @Nested
  class CategoryForeignKeyTests {
    private Table category;

    @BeforeEach
    void setUp() {
      category = Table.builder().name("category").columns(List.of()).foreignKeys(List.of()).build();
    }

    @Test
    void itHasNoForeignKeys() throws SQLException {
      final List<ForeignKey> fks = postgresqlQueryRunner.getForeignKeyInfo("public", category);
      assertTrue(fks.isEmpty());
    }
  }

  @Nested
  class ProductCategoryForeignKeyTests {
    private Table productCategory;

    @BeforeEach
    void setUp() {
      productCategory =
          Table.builder()
              .name("product_category")
              .columns(List.of())
              .foreignKeys(List.of())
              .build();
    }

    @Test
    void itHasForeignKeyToProduct() throws SQLException {
      final List<ForeignKey> fks =
          postgresqlQueryRunner.getForeignKeyInfo("public", productCategory);

      final ForeignKey fk =
          fks.stream().filter(f -> f.sourceColumn().equals("product_id")).findFirst().orElseThrow();

      assertEquals("product_category_product_id_fkey", fk.name());
      assertEquals("product", fk.targetTable());
      assertEquals("id", fk.targetColumn());
    }

    @Test
    void itHasForeignKeyToCategory() throws SQLException {
      final List<ForeignKey> fks =
          postgresqlQueryRunner.getForeignKeyInfo("public", productCategory);

      final ForeignKey fk =
          fks.stream()
              .filter(f -> f.sourceColumn().equals("category_id"))
              .findFirst()
              .orElseThrow();

      assertEquals("product_category_category_id_fkey", fk.name());
      assertEquals("category", fk.targetTable());
      assertEquals("id", fk.targetColumn());
    }
  }

  @Nested
  class CustomerOrderForeignKeyTests {
    private Table customerOrder;

    @BeforeEach
    void setUp() {
      customerOrder =
          Table.builder().name("customer_order").columns(List.of()).foreignKeys(List.of()).build();
    }

    @Test
    void itHasForeignKeyToAppUser() throws SQLException {
      final List<ForeignKey> fks = postgresqlQueryRunner.getForeignKeyInfo("public", customerOrder);

      final ForeignKey fk =
          fks.stream().filter(f -> f.sourceColumn().equals("user_id")).findFirst().orElseThrow();

      assertEquals("customer_order_user_id_fkey", fk.name());
      assertEquals("app_user", fk.targetTable());
      assertEquals("id", fk.targetColumn());
    }

    @Test
    void itHasForeignKeyToAddress() throws SQLException {
      final List<ForeignKey> fks = postgresqlQueryRunner.getForeignKeyInfo("public", customerOrder);

      final ForeignKey fk =
          fks.stream()
              .filter(f -> f.sourceColumn().equals("shipping_address_id"))
              .findFirst()
              .orElseThrow();

      assertEquals("customer_order_shipping_address_id_fkey", fk.name());
      assertEquals("address", fk.targetTable());
      assertEquals("id", fk.targetColumn());
    }
  }

  @Nested
  class OrderItemForeignKeyTests {
    private Table orderItem;

    @BeforeEach
    void setUp() {
      orderItem =
          Table.builder().name("order_item").columns(List.of()).foreignKeys(List.of()).build();
    }

    @Test
    void itHasForeignKeyToCustomerOrder() throws SQLException {
      final List<ForeignKey> fks = postgresqlQueryRunner.getForeignKeyInfo("public", orderItem);

      final ForeignKey fk =
          fks.stream().filter(f -> f.sourceColumn().equals("order_id")).findFirst().orElseThrow();

      assertEquals("order_item_order_id_fkey", fk.name());
      assertEquals("customer_order", fk.targetTable());
      assertEquals("id", fk.targetColumn());
    }

    @Test
    void itHasForeignKeyToProduct() throws SQLException {
      final List<ForeignKey> fks = postgresqlQueryRunner.getForeignKeyInfo("public", orderItem);

      final ForeignKey fk =
          fks.stream().filter(f -> f.sourceColumn().equals("product_id")).findFirst().orElseThrow();

      assertEquals("order_item_product_id_fkey", fk.name());
      assertEquals("product", fk.targetTable());
      assertEquals("id", fk.targetColumn());
    }
  }

  @Nested
  class PaymentForeignKeyTests {
    private Table payment;

    @BeforeEach
    void setUp() {
      payment = Table.builder().name("payment").columns(List.of()).foreignKeys(List.of()).build();
    }

    @Test
    void itHasForeignKeyToCustomerOrder() throws SQLException {
      final List<ForeignKey> fks = postgresqlQueryRunner.getForeignKeyInfo("public", payment);

      final ForeignKey fk =
          fks.stream().filter(f -> f.sourceColumn().equals("order_id")).findFirst().orElseThrow();

      assertEquals("payment_order_id_fkey", fk.name());
      assertEquals("customer_order", fk.targetTable());
      assertEquals("id", fk.targetColumn());
    }
  }

  @Nested
  class AuditLogForeignKeyTests {
    private Table auditLog;

    @BeforeEach
    void setUp() {
      auditLog =
          Table.builder().name("audit_log").columns(List.of()).foreignKeys(List.of()).build();
    }

    @Test
    void itHasForeignKeyToAppUser() throws SQLException {
      final List<ForeignKey> fks = postgresqlQueryRunner.getForeignKeyInfo("public", auditLog);

      final ForeignKey fk =
          fks.stream()
              .filter(f -> f.sourceColumn().equals("performed_by"))
              .findFirst()
              .orElseThrow();

      assertEquals("audit_log_performed_by_fkey", fk.name());
      assertEquals("app_user", fk.targetTable());
      assertEquals("id", fk.targetColumn());
    }
  }

  @Nested
  class TagLogForeignKeyTests {

    private Table tagLog;

    @BeforeEach
    void setUp() {
      tagLog = Table.builder().name("tag_log").columns(List.of()).foreignKeys(List.of()).build();
    }

    @Test
    void itHasNoForeignKeys() throws SQLException {
      final List<ForeignKey> foreignKeys =
          postgresqlQueryRunner.getForeignKeyInfo("public", tagLog);

      assertNotNull(foreignKeys);
      assertTrue(foreignKeys.isEmpty(), "tag_log should not have any foreign keys");
    }
  }

  @Nested
  class EnumInfoTests {

    @Test
    void itIdentifiesTheOrderStatusEnumNameAndColumnName() throws SQLException {
      List<DbEnum> result = postgresqlQueryRunner.getEnumInfo("public");

      assertEquals(1, result.size());
      DbEnum info = result.getFirst();

      assertEquals("status", info.columnName());
      assertEquals("order_status", info.enumName());
    }
  }

  @Nested
  class EnumValueTests {

    @Test
    void itIdentifiesTheOrderStatusEnumValues() throws SQLException {
      final var dbEnum =
          DbEnum.builder()
              .columnName("status")
              .enumName("order_status")
              .enumValues(List.of())
              .build();

      List<String> values = postgresqlQueryRunner.getEnumValues("public", dbEnum);

      assertEquals(5, values.size());
      assertEquals("pending", values.getFirst());
      assertEquals("paid", values.get(1));
      assertEquals("shipped", values.get(2));
      assertEquals("cancelled", values.get(3));
      assertEquals("refunded", values.get(4));
    }
  }
}
