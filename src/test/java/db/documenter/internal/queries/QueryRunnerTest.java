package db.documenter.internal.queries;

import static org.junit.jupiter.api.Assertions.*;

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

    @Nested
    class AppUserColumnTests {

      private Table appUser;

      @BeforeEach
      void setUp() {
        appUser = Table.builder().schema("public").name("app_user").build();
      }

      @Test
      void itReturnsColumns() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", appUser);
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
      void columnsHaveCorrectOrdinalPosition() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", appUser);

        final var idColumn = columns.getFirst();
        final var userNameColumn = columns.get(1);
        final var emailColumn = columns.get(2);
        final var displayNameColumn = columns.get(3);
        final var birthDateColumn = columns.get(4);
        final var createdAtColumn = columns.get(5);
        final var profileColumn = columns.get(6);
        final var isActiveColumn = columns.getLast();

        assertEquals(8, columns.size());

        assertEquals(1, idColumn.ordinalPosition());
        assertEquals(2, userNameColumn.ordinalPosition());
        assertEquals(3, emailColumn.ordinalPosition());
        assertEquals(4, displayNameColumn.ordinalPosition());
        assertEquals(5, birthDateColumn.ordinalPosition());
        assertEquals(6, createdAtColumn.ordinalPosition());
        assertEquals(7, profileColumn.ordinalPosition());
        assertEquals(8, isActiveColumn.ordinalPosition());
      }

      @Test
      void columnsHaveCorrectIsNullable() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", appUser);

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
        final List<Column> columns = queryRunner.getColumnInfo("public", appUser);

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
        final List<Column> columns = queryRunner.getColumnInfo("public", appUser);

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
        roleTable = Table.builder().schema("public").name("role").build();
      }

      @Test
      void itReturnsColumns() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", roleTable);
        final List<String> names = columns.stream().map(Column::name).toList();

        final List<String> expectedColumnNames = List.of("id", "code", "description");

        assertTrue(names.containsAll(expectedColumnNames));
        assertEquals(expectedColumnNames.size(), names.size());
      }

      @Test
      void columnsHaveCorrectOrdinalPosition() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", roleTable);

        final var idColumn = columns.getFirst();
        final var codeColumn = columns.get(1);
        final var descriptionColumn = columns.getLast();

        assertEquals(3, columns.size());

        assertEquals(1, idColumn.ordinalPosition());
        assertEquals(2, codeColumn.ordinalPosition());
        assertEquals(3, descriptionColumn.ordinalPosition());
      }

      @Test
      void columnsHaveCorrectIsNullable() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", roleTable);

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
        final List<Column> columns = queryRunner.getColumnInfo("public", roleTable);

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
        final List<Column> columns = queryRunner.getColumnInfo("public", roleTable);

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
        userRole = Table.builder().schema("public").name("user_role").build();
      }

      @Test
      void itReturnsColumns() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", userRole);
        final List<String> names = columns.stream().map(Column::name).toList();

        final List<String> expectedColumnNames =
            List.of("user_id", "role_id", "assigned_at", "assigned_by");

        assertTrue(names.containsAll(expectedColumnNames));
        assertEquals(expectedColumnNames.size(), names.size());
      }

      @Test
      void columnsHaveCorrectOrdinalPosition() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", userRole);

        final var userIdColumn = columns.getFirst();
        final var roleIdColumn = columns.get(1);
        final var assignedAtColumn = columns.get(2);
        final var assignedByColumn = columns.getLast();

        assertEquals(4, columns.size());

        assertEquals(1, userIdColumn.ordinalPosition());
        assertEquals(2, roleIdColumn.ordinalPosition());
        assertEquals(3, assignedAtColumn.ordinalPosition());
        assertEquals(4, assignedByColumn.ordinalPosition());
      }

      @Test
      void columnsHaveCorrectIsNullable() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", userRole);

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
        final List<Column> columns = queryRunner.getColumnInfo("public", userRole);

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
        final List<Column> columns = queryRunner.getColumnInfo("public", userRole);

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
        address = Table.builder().schema("public").name("address").build();
      }

      @Test
      void itReturnsColumns() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", address);
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
      void columnsHaveCorrectOrdinalPosition() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", address);

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

        assertEquals(1, idColumn.ordinalPosition());
        assertEquals(2, userIdColumn.ordinalPosition());
        assertEquals(3, line1Column.ordinalPosition());
        assertEquals(4, line2Column.ordinalPosition());
        assertEquals(5, cityColumn.ordinalPosition());
        assertEquals(6, stateColumn.ordinalPosition());
        assertEquals(7, postalCodeColumn.ordinalPosition());
        assertEquals(8, countryColumn.ordinalPosition());
        assertEquals(9, createdAtColumn.ordinalPosition());
      }

      @Test
      void columnsHaveCorrectIsNullable() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", address);

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
        final List<Column> columns = queryRunner.getColumnInfo("public", address);

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
        final List<Column> columns = queryRunner.getColumnInfo("public", address);

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
        product = Table.builder().schema("public").name("product").build();
      }

      @Test
      void itReturnsColumns() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", product);
        final List<String> names = columns.stream().map(Column::name).toList();

        final List<String> expectedColumnNames =
            List.of(
                "id", "sku", "name", "description", "price", "attributes", "created_at", "stock");

        assertTrue(names.containsAll(expectedColumnNames));
        assertEquals(expectedColumnNames.size(), names.size());
      }

      @Test
      void columnsHaveCorrectOrdinalPosition() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", product);

        final var idColumn = columns.getFirst();
        final var skuColumn = columns.get(1);
        final var nameColumn = columns.get(2);
        final var descriptionColumn = columns.get(3);
        final var priceColumn = columns.get(4);
        final var attributesColumn = columns.get(5);
        final var createdAtColumn = columns.get(6);
        final var stockColumn = columns.getLast();

        assertEquals(8, columns.size());

        assertEquals(1, idColumn.ordinalPosition());
        assertEquals(2, skuColumn.ordinalPosition());
        assertEquals(3, nameColumn.ordinalPosition());
        assertEquals(4, descriptionColumn.ordinalPosition());
        assertEquals(5, priceColumn.ordinalPosition());
        assertEquals(6, attributesColumn.ordinalPosition());
        assertEquals(7, createdAtColumn.ordinalPosition());
        assertEquals(8, stockColumn.ordinalPosition());
      }

      @Test
      void columnsHaveCorrectIsNullable() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", product);

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
        final List<Column> columns = queryRunner.getColumnInfo("public", product);

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
        final List<Column> columns = queryRunner.getColumnInfo("public", product);

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
        category = Table.builder().schema("public").name("category").build();
      }

      @Test
      void itReturnsColumns() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", category);
        final List<String> names = columns.stream().map(Column::name).toList();

        final List<String> expectedColumnNames = List.of("id", "slug", "title", "metadata");

        assertTrue(names.containsAll(expectedColumnNames));
        assertEquals(expectedColumnNames.size(), names.size());
      }

      @Test
      void columnsHaveCorrectOrdinalPosition() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", category);

        final var idColumn = columns.getFirst();
        final var slugColumn = columns.get(1);
        final var titleColumn = columns.get(2);
        final var metadataColumn = columns.getLast();

        assertEquals(4, columns.size());

        assertEquals(1, idColumn.ordinalPosition());
        assertEquals(2, slugColumn.ordinalPosition());
        assertEquals(3, titleColumn.ordinalPosition());
        assertEquals(4, metadataColumn.ordinalPosition());
      }

      @Test
      void columnsHaveCorrectIsNullable() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", category);

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
        final List<Column> columns = queryRunner.getColumnInfo("public", category);

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
        final List<Column> columns = queryRunner.getColumnInfo("public", category);

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
        productCategory = Table.builder().schema("public").name("product_category").build();
      }

      @Test
      void itReturnsColumns() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", productCategory);
        final List<String> names = columns.stream().map(Column::name).toList();

        final List<String> expectedColumnNames =
            List.of("product_id", "category_id", "assigned_at");

        assertTrue(names.containsAll(expectedColumnNames));
        assertEquals(expectedColumnNames.size(), names.size());
      }

      @Test
      void columnsHaveCorrectOrdinalPosition() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", productCategory);

        final var productIdColumn = columns.getFirst();
        final var categoryIdColumn = columns.get(1);
        final var assignedAtColumn = columns.getLast();

        assertEquals(3, columns.size());

        assertEquals(1, productIdColumn.ordinalPosition());
        assertEquals(2, categoryIdColumn.ordinalPosition());
        assertEquals(3, assignedAtColumn.ordinalPosition());
      }

      @Test
      void columnsHaveCorrectIsNullable() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", productCategory);

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
        final List<Column> columns = queryRunner.getColumnInfo("public", productCategory);

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
        final List<Column> columns = queryRunner.getColumnInfo("public", productCategory);

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
        customerOrder = Table.builder().schema("public").name("customer_order").build();
      }

      @Test
      void itReturnsColumns() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", customerOrder);
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
      void columnsHaveCorrectOrdinalPosition() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", customerOrder);

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

        assertEquals(1, idColumn.ordinalPosition());
        assertEquals(2, userIdColumn.ordinalPosition());
        assertEquals(3, orderNumberColumn.ordinalPosition());
        assertEquals(4, orderDateColumn.ordinalPosition());
        assertEquals(5, shipDateColumn.ordinalPosition());
        assertEquals(6, shippingAddressIdColumn.ordinalPosition());
        assertEquals(7, statusColumn.ordinalPosition());
        assertEquals(8, totalColumn.ordinalPosition());
        assertEquals(9, metadataColumn.ordinalPosition());
      }

      @Test
      void columnsHaveCorrectIsNullable() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", customerOrder);

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
        final List<Column> columns = queryRunner.getColumnInfo("public", customerOrder);

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
        final List<Column> columns = queryRunner.getColumnInfo("public", customerOrder);

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
        orderItem = Table.builder().schema("public").name("order_item").build();
      }

      @Test
      void itReturnsColumns() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", orderItem);
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
      void columnsHaveCorrectOrdinalPosition() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", orderItem);

        final var idColumn = columns.getFirst();
        final var orderIdColumn = columns.get(1);
        final var productIdColumn = columns.get(2);
        final var productSnapshotColumn = columns.get(3);
        final var unitPriceColumn = columns.get(4);
        final var quantityColumn = columns.get(5);
        final var lineTotalColumn = columns.getLast();

        assertEquals(7, columns.size());

        assertEquals(1, idColumn.ordinalPosition());
        assertEquals(2, orderIdColumn.ordinalPosition());
        assertEquals(3, productIdColumn.ordinalPosition());
        assertEquals(4, productSnapshotColumn.ordinalPosition());
        assertEquals(5, unitPriceColumn.ordinalPosition());
        assertEquals(6, quantityColumn.ordinalPosition());
        assertEquals(7, lineTotalColumn.ordinalPosition());
      }

      @Test
      void columnsHaveCorrectIsNullable() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", orderItem);

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
        final List<Column> columns = queryRunner.getColumnInfo("public", orderItem);

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
        final List<Column> columns = queryRunner.getColumnInfo("public", orderItem);

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
        payment = Table.builder().schema("public").name("payment").build();
      }

      @Test
      void itReturnsColumns() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", payment);
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
      void columnsHaveCorrectOrdinalPosition() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", payment);

        final var idColumn = columns.getFirst();
        final var orderIdColumn = columns.get(1);
        final var paidAtColumn = columns.get(2);
        final var amountColumn = columns.get(3);
        final var methodColumn = columns.get(4);
        final var providerTransactionIdColumn = columns.get(5);
        final var rawResponseColumn = columns.getLast();

        assertEquals(7, columns.size());

        assertEquals(1, idColumn.ordinalPosition());
        assertEquals(2, orderIdColumn.ordinalPosition());
        assertEquals(3, paidAtColumn.ordinalPosition());
        assertEquals(4, amountColumn.ordinalPosition());
        assertEquals(5, methodColumn.ordinalPosition());
        assertEquals(6, providerTransactionIdColumn.ordinalPosition());
        assertEquals(7, rawResponseColumn.ordinalPosition());
      }

      @Test
      void columnsHaveCorrectIsNullable() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", payment);

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
        final List<Column> columns = queryRunner.getColumnInfo("public", payment);

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
        final List<Column> columns = queryRunner.getColumnInfo("public", payment);

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
        auditLog = Table.builder().schema("public").name("audit_log").build();
      }

      @Test
      void itReturnsColumns() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", auditLog);
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
      void columnsHaveCorrectOrdinalPosition() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", auditLog);

        final var idColumn = columns.getFirst();
        final var entityTypeColumn = columns.get(1);
        final var entityIdColumn = columns.get(2);
        final var actionColumn = columns.get(3);
        final var performedByColumn = columns.get(4);
        final var performedAtColumn = columns.get(5);
        final var detailsColumn = columns.getLast();

        assertEquals(7, columns.size());

        assertEquals(1, idColumn.ordinalPosition());
        assertEquals(2, entityTypeColumn.ordinalPosition());
        assertEquals(3, entityIdColumn.ordinalPosition());
        assertEquals(4, actionColumn.ordinalPosition());
        assertEquals(5, performedByColumn.ordinalPosition());
        assertEquals(6, performedAtColumn.ordinalPosition());
        assertEquals(7, detailsColumn.ordinalPosition());
      }

      @Test
      void columnsHaveCorrectIsNullable() throws SQLException {
        final List<Column> columns = queryRunner.getColumnInfo("public", auditLog);

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
        final List<Column> columns = queryRunner.getColumnInfo("public", auditLog);

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
        final List<Column> columns = queryRunner.getColumnInfo("public", auditLog);

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
  }

  @Nested
  class PrimaryKeyTests {

    private Table appUser;

    @BeforeEach
    void setUp() {
      appUser = Table.builder().schema("public").name("app_user").build();
    }

    @Nested
    class AppUserPrimaryKeyTests {

      @Test
      void itHasTheCorrectConstraintName() throws SQLException {
        final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo("public", appUser);

        assertEquals("app_user_pkey", primaryKey.constraintName());
      }

      @Test
      void itHasTheCorrectColumnNames() throws SQLException {
        final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo("public", appUser);

        assertEquals(1, primaryKey.columnNames().size());
        assertEquals("id", primaryKey.columnNames().getFirst());
      }
    }

    @Nested
    class RolePrimaryKeyTests {

      private Table role;

      @BeforeEach
      void setUp() {
        role = Table.builder().schema("public").name("role").build();
      }

      @Test
      void itHasTheCorrectConstraintName() throws SQLException {
        final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo("public", role);

        assertEquals("role_pkey", primaryKey.constraintName());
      }

      @Test
      void itHasTheCorrectColumnNames() throws SQLException {
        final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo("public", role);

        assertEquals(1, primaryKey.columnNames().size());
        assertEquals("id", primaryKey.columnNames().getFirst());
      }
    }

    @Nested
    class UserRolePrimaryKeyTests {

      private Table userRole;

      @BeforeEach
      void setUp() {
        userRole = Table.builder().schema("public").name("user_role").build();
      }

      @Test
      void itHasTheCorrectConstraintName() throws SQLException {
        final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo("public", userRole);

        assertEquals("user_role_pkey", primaryKey.constraintName());
      }

      @Test
      void itHasTheCorrectColumnNames() throws SQLException {
        final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo("public", userRole);

        assertEquals(2, primaryKey.columnNames().size());
        assertEquals("user_id", primaryKey.columnNames().get(0));
        assertEquals("role_id", primaryKey.columnNames().get(1));
      }
    }

    @Nested
    class AddressPrimaryKeyTests {

      private Table address;

      @BeforeEach
      void setUp() {
        address = Table.builder().schema("public").name("address").build();
      }

      @Test
      void itHasTheCorrectConstraintName() throws SQLException {
        final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo("public", address);

        assertEquals("address_pkey", primaryKey.constraintName());
      }

      @Test
      void itHasTheCorrectColumnNames() throws SQLException {
        final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo("public", address);

        assertEquals(1, primaryKey.columnNames().size());
        assertEquals("id", primaryKey.columnNames().getFirst());
      }
    }

    @Nested
    class ProductPrimaryKeyTests {

      private Table product;

      @BeforeEach
      void setUp() {
        product = Table.builder().schema("public").name("product").build();
      }

      @Test
      void itHasTheCorrectConstraintName() throws SQLException {
        final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo("public", product);

        assertEquals("product_pkey", primaryKey.constraintName());
      }

      @Test
      void itHasTheCorrectColumnNames() throws SQLException {
        final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo("public", product);

        assertEquals(1, primaryKey.columnNames().size());
        assertEquals("id", primaryKey.columnNames().getFirst());
      }
    }

    @Nested
    class CategoryPrimaryKeyTests {

      private Table category;

      @BeforeEach
      void setUp() {
        category = Table.builder().schema("public").name("category").build();
      }

      @Test
      void itHasTheCorrectConstraintName() throws SQLException {
        final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo("public", category);

        assertEquals("category_pkey", primaryKey.constraintName());
      }

      @Test
      void itHasTheCorrectColumnNames() throws SQLException {
        final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo("public", category);

        assertEquals(1, primaryKey.columnNames().size());
        assertEquals("id", primaryKey.columnNames().getFirst());
      }
    }

    @Nested
    class ProductCategoryPrimaryKeyTests {

      private Table productCategory;

      @BeforeEach
      void setUp() {
        productCategory = Table.builder().schema("public").name("product_category").build();
      }

      @Test
      void itHasTheCorrectConstraintName() throws SQLException {
        final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo("public", productCategory);

        assertEquals("product_category_pkey", primaryKey.constraintName());
      }

      @Test
      void itHasTheCorrectColumnNames() throws SQLException {
        final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo("public", productCategory);

        assertEquals(2, primaryKey.columnNames().size());
        assertEquals("product_id", primaryKey.columnNames().get(0));
        assertEquals("category_id", primaryKey.columnNames().get(1));
      }
    }

    @Nested
    class CustomerOrderPrimaryKeyTests {

      private Table customerOrder;

      @BeforeEach
      void setUp() {
        customerOrder = Table.builder().schema("public").name("customer_order").build();
      }

      @Test
      void itHasTheCorrectConstraintName() throws SQLException {
        final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo("public", customerOrder);

        assertEquals("customer_order_pkey", primaryKey.constraintName());
      }

      @Test
      void itHasTheCorrectColumnNames() throws SQLException {
        final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo("public", customerOrder);

        assertEquals(1, primaryKey.columnNames().size());
        assertEquals("id", primaryKey.columnNames().getFirst());
      }
    }

    @Nested
    class OrderItemPrimaryKeyTests {

      private Table orderItem;

      @BeforeEach
      void setUp() {
        orderItem = Table.builder().schema("public").name("order_item").build();
      }

      @Test
      void itHasTheCorrectConstraintName() throws SQLException {
        final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo("public", orderItem);

        assertEquals("order_item_pkey", primaryKey.constraintName());
      }

      @Test
      void itHasTheCorrectColumnNames() throws SQLException {
        final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo("public", orderItem);

        assertEquals(1, primaryKey.columnNames().size());
        assertEquals("id", primaryKey.columnNames().getFirst());
      }
    }

    @Nested
    class PaymentPrimaryKeyTests {

      private Table payment;

      @BeforeEach
      void setUp() {
        payment = Table.builder().schema("public").name("payment").build();
      }

      @Test
      void itHasTheCorrectConstraintName() throws SQLException {
        final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo("public", payment);

        assertEquals("payment_pkey", primaryKey.constraintName());
      }

      @Test
      void itHasTheCorrectColumnNames() throws SQLException {
        final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo("public", payment);

        assertEquals(1, primaryKey.columnNames().size());
        assertEquals("id", primaryKey.columnNames().getFirst());
      }
    }

    @Nested
    class AuditLogPrimaryKeyTests {

      private Table auditLog;

      @BeforeEach
      void setUp() {
        auditLog = Table.builder().schema("public").name("audit_log").build();
      }

      @Test
      void itHasTheCorrectConstraintName() throws SQLException {
        final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo("public", auditLog);

        assertEquals("audit_log_pkey", primaryKey.constraintName());
      }

      @Test
      void itHasTheCorrectColumnNames() throws SQLException {
        final PrimaryKey primaryKey = queryRunner.getPrimaryKeyInfo("public", auditLog);

        assertEquals(1, primaryKey.columnNames().size());
        assertEquals("id", primaryKey.columnNames().getFirst());
      }
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
