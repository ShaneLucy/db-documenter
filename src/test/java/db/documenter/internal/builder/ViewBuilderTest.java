package db.documenter.internal.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import db.documenter.internal.mapper.ColumnMapper;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ColumnKey;
import db.documenter.internal.models.db.Constraint;
import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.MaterializedView;
import db.documenter.internal.models.db.View;
import db.documenter.internal.models.db.postgresql.EnumKey;
import db.documenter.internal.models.db.postgresql.UdtReference;
import db.documenter.internal.queries.api.QueryRunner;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ViewBuilderTest {

  @Mock private QueryRunner queryRunner;

  private ViewBuilder viewBuilder;

  @BeforeEach
  void setUp() {
    reset(queryRunner);
    viewBuilder = new ViewBuilder(new ColumnMapper());
  }

  @Nested
  class BuildViewsTests {

    @Test
    void returnsEmptyListWhenNoViewsExist() throws SQLException {
      when(queryRunner.getViewInfo("test_schema")).thenReturn(List.of());

      final List<View> result =
          viewBuilder.buildViews(queryRunner, "test_schema", Map.of(), Map.of());

      assertTrue(result.isEmpty());
    }

    @Test
    void buildsSingleViewWithColumnsFromQueryRunner() throws SQLException {
      final var stubView = View.builder().name("active_users_view").columns(List.of()).build();

      final var idColumn =
          Column.builder()
              .name("id")
              .dataType("uuid")
              .maximumLength(0)
              .constraints(List.of())
              .build();
      final var nameColumn =
          Column.builder()
              .name("name")
              .dataType("text")
              .maximumLength(0)
              .constraints(List.of())
              .build();

      when(queryRunner.getViewInfo("test_schema")).thenReturn(List.of(stubView));
      when(queryRunner.getColumnInfo("test_schema", "active_users_view"))
          .thenReturn(List.of(idColumn, nameColumn));

      final List<View> result =
          viewBuilder.buildViews(queryRunner, "test_schema", Map.of(), Map.of());

      assertEquals(1, result.size());
      assertEquals("active_users_view", result.getFirst().name());
      assertEquals(2, result.getFirst().columns().size());
      assertEquals("id", result.getFirst().columns().getFirst().name());
      assertEquals("uuid", result.getFirst().columns().getFirst().dataType());
      assertEquals("name", result.getFirst().columns().get(1).name());
    }

    @Test
    void buildsMultipleViewsWithTheirOwnColumns() throws SQLException {
      final var stubView1 = View.builder().name("user_view").columns(List.of()).build();
      final var stubView2 = View.builder().name("order_view").columns(List.of()).build();

      final var userIdColumn =
          Column.builder()
              .name("user_id")
              .dataType("uuid")
              .maximumLength(0)
              .constraints(List.of())
              .build();
      final var orderIdColumn =
          Column.builder()
              .name("order_id")
              .dataType("integer")
              .maximumLength(0)
              .constraints(List.of())
              .build();

      when(queryRunner.getViewInfo("public")).thenReturn(List.of(stubView1, stubView2));
      when(queryRunner.getColumnInfo("public", "user_view")).thenReturn(List.of(userIdColumn));
      when(queryRunner.getColumnInfo("public", "order_view")).thenReturn(List.of(orderIdColumn));

      final List<View> result = viewBuilder.buildViews(queryRunner, "public", Map.of(), Map.of());

      assertEquals(2, result.size());
      assertEquals("user_view", result.getFirst().name());
      assertEquals(1, result.getFirst().columns().size());
      assertEquals("user_id", result.getFirst().columns().getFirst().name());
      assertEquals("order_view", result.get(1).name());
      assertEquals(1, result.get(1).columns().size());
      assertEquals("order_id", result.get(1).columns().getFirst().name());
    }

    @Test
    void buildsViewWithEmptyColumnList() throws SQLException {
      final var stubView = View.builder().name("empty_view").columns(List.of()).build();

      when(queryRunner.getViewInfo("test_schema")).thenReturn(List.of(stubView));
      when(queryRunner.getColumnInfo("test_schema", "empty_view")).thenReturn(List.of());

      final List<View> result =
          viewBuilder.buildViews(queryRunner, "test_schema", Map.of(), Map.of());

      assertEquals(1, result.size());
      assertEquals("empty_view", result.getFirst().name());
      assertTrue(result.getFirst().columns().isEmpty());
    }

    @Test
    void resolvesUserDefinedTypeToEnumNameInSameSchema() throws SQLException {
      final var stubView = View.builder().name("order_view").columns(List.of()).build();

      // Column with USER-DEFINED type (a PostgreSQL enum in the same schema)
      final var statusColumn =
          Column.builder()
              .name("status")
              .dataType("USER-DEFINED")
              .maximumLength(0)
              .constraints(List.of())
              .build();

      final var enumDef =
          DbEnum.builder()
              .schemaName("core")
              .enumName("order_status")
              .enumValues(List.of("PENDING", "SHIPPED"))
              .build();

      final var columnKey = new ColumnKey("order_view", "status");
      final var udtRef = new UdtReference("core", "order_status");
      final var enumKey = new EnumKey("core", "order_status");

      when(queryRunner.getViewInfo("core")).thenReturn(List.of(stubView));
      when(queryRunner.getColumnInfo("core", "order_view")).thenReturn(List.of(statusColumn));

      final List<View> result =
          viewBuilder.buildViews(
              queryRunner, "core", Map.of(enumKey, enumDef), Map.of(columnKey, udtRef));

      assertEquals(1, result.size());
      assertEquals(1, result.getFirst().columns().size());
      // Same-schema UDT resolution should produce an unqualified type name
      assertEquals("order_status", result.getFirst().columns().getFirst().dataType());
    }

    @Test
    void resolvesUserDefinedTypeToCrossSchemaQualifiedName() throws SQLException {
      final var stubView = View.builder().name("report_view").columns(List.of()).build();

      final var statusColumn =
          Column.builder()
              .name("status")
              .dataType("USER-DEFINED")
              .maximumLength(0)
              .constraints(List.of())
              .build();

      final var enumDef =
          DbEnum.builder()
              .schemaName("auth")
              .enumName("account_status")
              .enumValues(List.of("ACTIVE", "SUSPENDED"))
              .build();

      // Column is in "core" schema, but the enum is in "auth" schema
      final var columnKey = new ColumnKey("report_view", "status");
      final var udtRef = new UdtReference("auth", "account_status");
      final var enumKey = new EnumKey("auth", "account_status");

      when(queryRunner.getViewInfo("core")).thenReturn(List.of(stubView));
      when(queryRunner.getColumnInfo("core", "report_view")).thenReturn(List.of(statusColumn));

      final List<View> result =
          viewBuilder.buildViews(
              queryRunner, "core", Map.of(enumKey, enumDef), Map.of(columnKey, udtRef));

      assertEquals(1, result.size());
      assertEquals(1, result.getFirst().columns().size());
      // Cross-schema UDT resolution should produce a schema-qualified type name
      assertEquals("auth.account_status", result.getFirst().columns().getFirst().dataType());
    }

    @Test
    void preservesNonUserDefinedColumnTypesUnchanged() throws SQLException {
      final var stubView = View.builder().name("simple_view").columns(List.of()).build();

      final var idColumn =
          Column.builder()
              .name("id")
              .dataType("integer")
              .maximumLength(0)
              .constraints(List.of())
              .build();
      final var nameColumn =
          Column.builder()
              .name("name")
              .dataType("varchar")
              .maximumLength(200)
              .constraints(List.of(Constraint.NULLABLE))
              .build();

      when(queryRunner.getViewInfo("test_schema")).thenReturn(List.of(stubView));
      when(queryRunner.getColumnInfo("test_schema", "simple_view"))
          .thenReturn(List.of(idColumn, nameColumn));

      final List<View> result =
          viewBuilder.buildViews(queryRunner, "test_schema", Map.of(), Map.of());

      assertEquals(1, result.size());
      assertEquals("integer", result.getFirst().columns().getFirst().dataType());
      assertEquals("varchar", result.getFirst().columns().get(1).dataType());
      assertEquals(200, result.getFirst().columns().get(1).maximumLength());
    }

    @Test
    void propagatesSQLExceptionFromGetViewInfo() throws SQLException {
      when(queryRunner.getViewInfo("failing_schema"))
          .thenThrow(new SQLException("Connection refused"));

      final SQLException exception =
          assertThrows(
              SQLException.class,
              () -> viewBuilder.buildViews(queryRunner, "failing_schema", Map.of(), Map.of()));

      assertEquals("Connection refused", exception.getMessage());
    }

    @Test
    void propagatesSQLExceptionFromGetColumnInfo() throws SQLException {
      final var stubView = View.builder().name("problem_view").columns(List.of()).build();

      when(queryRunner.getViewInfo("test_schema")).thenReturn(List.of(stubView));
      when(queryRunner.getColumnInfo("test_schema", "problem_view"))
          .thenThrow(new SQLException("Timeout querying columns"));

      final SQLException exception =
          assertThrows(
              SQLException.class,
              () -> viewBuilder.buildViews(queryRunner, "test_schema", Map.of(), Map.of()));

      assertEquals("Timeout querying columns", exception.getMessage());
    }
  }

  @Nested
  class BuildMaterializedViewsTests {

    @Test
    void returnsEmptyListWhenNoMaterializedViewsExist() throws SQLException {
      when(queryRunner.getMaterializedViewInfo("test_schema")).thenReturn(List.of());

      final List<MaterializedView> result =
          viewBuilder.buildMaterializedViews(queryRunner, "test_schema", Map.of(), Map.of());

      assertTrue(result.isEmpty());
    }

    @Test
    void buildsSingleMaterializedViewWithColumnsFromQueryRunner() throws SQLException {
      final var stubMatView =
          MaterializedView.builder().name("monthly_sales_summary").columns(List.of()).build();

      final var monthColumn =
          Column.builder()
              .name("month")
              .dataType("date")
              .maximumLength(0)
              .constraints(List.of())
              .build();
      final var revenueColumn =
          Column.builder()
              .name("total_revenue")
              .dataType("numeric")
              .maximumLength(0)
              .constraints(List.of())
              .build();

      when(queryRunner.getMaterializedViewInfo("analytics")).thenReturn(List.of(stubMatView));
      when(queryRunner.getColumnInfo("analytics", "monthly_sales_summary"))
          .thenReturn(List.of(monthColumn, revenueColumn));

      final List<MaterializedView> result =
          viewBuilder.buildMaterializedViews(queryRunner, "analytics", Map.of(), Map.of());

      assertEquals(1, result.size());
      assertEquals("monthly_sales_summary", result.getFirst().name());
      assertEquals(2, result.getFirst().columns().size());
      assertEquals("month", result.getFirst().columns().getFirst().name());
      assertEquals("date", result.getFirst().columns().getFirst().dataType());
      assertEquals("total_revenue", result.getFirst().columns().get(1).name());
    }

    @Test
    void buildsMultipleMaterializedViewsWithTheirOwnColumns() throws SQLException {
      final var stubMatView1 =
          MaterializedView.builder().name("sales_summary").columns(List.of()).build();
      final var stubMatView2 =
          MaterializedView.builder().name("inventory_snapshot").columns(List.of()).build();

      final var salesColumn =
          Column.builder()
              .name("sales_amount")
              .dataType("numeric")
              .maximumLength(0)
              .constraints(List.of())
              .build();
      final var inventoryColumn =
          Column.builder()
              .name("stock_count")
              .dataType("integer")
              .maximumLength(0)
              .constraints(List.of())
              .build();

      when(queryRunner.getMaterializedViewInfo("reporting"))
          .thenReturn(List.of(stubMatView1, stubMatView2));
      when(queryRunner.getColumnInfo("reporting", "sales_summary"))
          .thenReturn(List.of(salesColumn));
      when(queryRunner.getColumnInfo("reporting", "inventory_snapshot"))
          .thenReturn(List.of(inventoryColumn));

      final List<MaterializedView> result =
          viewBuilder.buildMaterializedViews(queryRunner, "reporting", Map.of(), Map.of());

      assertEquals(2, result.size());
      assertEquals("sales_summary", result.getFirst().name());
      assertEquals(1, result.getFirst().columns().size());
      assertEquals("sales_amount", result.getFirst().columns().getFirst().name());
      assertEquals("inventory_snapshot", result.get(1).name());
      assertEquals(1, result.get(1).columns().size());
      assertEquals("stock_count", result.get(1).columns().getFirst().name());
    }

    @Test
    void buildsMaterializedViewWithEmptyColumnList() throws SQLException {
      final var stubMatView =
          MaterializedView.builder().name("empty_mat_view").columns(List.of()).build();

      when(queryRunner.getMaterializedViewInfo("test_schema")).thenReturn(List.of(stubMatView));
      when(queryRunner.getColumnInfo("test_schema", "empty_mat_view")).thenReturn(List.of());

      final List<MaterializedView> result =
          viewBuilder.buildMaterializedViews(queryRunner, "test_schema", Map.of(), Map.of());

      assertEquals(1, result.size());
      assertEquals("empty_mat_view", result.getFirst().name());
      assertTrue(result.getFirst().columns().isEmpty());
    }

    @Test
    void resolvesUserDefinedTypeToEnumNameInSameSchema() throws SQLException {
      final var stubMatView =
          MaterializedView.builder().name("order_summary").columns(List.of()).build();

      final var statusColumn =
          Column.builder()
              .name("status")
              .dataType("USER-DEFINED")
              .maximumLength(0)
              .constraints(List.of())
              .build();

      final var enumDef =
          DbEnum.builder()
              .schemaName("core")
              .enumName("order_status")
              .enumValues(List.of("PENDING", "DELIVERED"))
              .build();

      final var columnKey = new ColumnKey("order_summary", "status");
      final var udtRef = new UdtReference("core", "order_status");
      final var enumKey = new EnumKey("core", "order_status");

      when(queryRunner.getMaterializedViewInfo("core")).thenReturn(List.of(stubMatView));
      when(queryRunner.getColumnInfo("core", "order_summary")).thenReturn(List.of(statusColumn));

      final List<MaterializedView> result =
          viewBuilder.buildMaterializedViews(
              queryRunner, "core", Map.of(enumKey, enumDef), Map.of(columnKey, udtRef));

      assertEquals(1, result.size());
      assertEquals(1, result.getFirst().columns().size());
      // Same-schema UDT resolution should produce an unqualified type name
      assertEquals("order_status", result.getFirst().columns().getFirst().dataType());
    }

    @Test
    void resolvesUserDefinedTypeToCrossSchemaQualifiedName() throws SQLException {
      final var stubMatView =
          MaterializedView.builder().name("account_report").columns(List.of()).build();

      final var statusColumn =
          Column.builder()
              .name("account_status")
              .dataType("USER-DEFINED")
              .maximumLength(0)
              .constraints(List.of())
              .build();

      final var enumDef =
          DbEnum.builder()
              .schemaName("auth")
              .enumName("account_state")
              .enumValues(List.of("ACTIVE", "LOCKED"))
              .build();

      // Materialized view is in "reporting" schema, but the enum is in "auth" schema
      final var columnKey = new ColumnKey("account_report", "account_status");
      final var udtRef = new UdtReference("auth", "account_state");
      final var enumKey = new EnumKey("auth", "account_state");

      when(queryRunner.getMaterializedViewInfo("reporting")).thenReturn(List.of(stubMatView));
      when(queryRunner.getColumnInfo("reporting", "account_report"))
          .thenReturn(List.of(statusColumn));

      final List<MaterializedView> result =
          viewBuilder.buildMaterializedViews(
              queryRunner, "reporting", Map.of(enumKey, enumDef), Map.of(columnKey, udtRef));

      assertEquals(1, result.size());
      assertEquals(1, result.getFirst().columns().size());
      // Cross-schema UDT resolution should produce a schema-qualified type name
      assertEquals("auth.account_state", result.getFirst().columns().getFirst().dataType());
    }

    @Test
    void preservesNonUserDefinedColumnTypesUnchanged() throws SQLException {
      final var stubMatView =
          MaterializedView.builder().name("plain_summary").columns(List.of()).build();

      final var timestampColumn =
          Column.builder()
              .name("created_at")
              .dataType("timestamp without time zone")
              .maximumLength(0)
              .constraints(List.of())
              .build();
      final var labelColumn =
          Column.builder()
              .name("label")
              .dataType("character varying")
              .maximumLength(100)
              .constraints(List.of(Constraint.NULLABLE))
              .build();

      when(queryRunner.getMaterializedViewInfo("test_schema")).thenReturn(List.of(stubMatView));
      when(queryRunner.getColumnInfo("test_schema", "plain_summary"))
          .thenReturn(List.of(timestampColumn, labelColumn));

      final List<MaterializedView> result =
          viewBuilder.buildMaterializedViews(queryRunner, "test_schema", Map.of(), Map.of());

      assertEquals(1, result.size());
      assertEquals(
          "timestamp without time zone", result.getFirst().columns().getFirst().dataType());
      assertEquals("character varying", result.getFirst().columns().get(1).dataType());
      assertEquals(100, result.getFirst().columns().get(1).maximumLength());
    }

    @Test
    void propagatesSQLExceptionFromGetMaterializedViewInfo() throws SQLException {
      when(queryRunner.getMaterializedViewInfo("failing_schema"))
          .thenThrow(new SQLException("Database unavailable"));

      final SQLException exception =
          assertThrows(
              SQLException.class,
              () ->
                  viewBuilder.buildMaterializedViews(
                      queryRunner, "failing_schema", Map.of(), Map.of()));

      assertEquals("Database unavailable", exception.getMessage());
    }

    @Test
    void propagatesSQLExceptionFromGetColumnInfo() throws SQLException {
      final var stubMatView =
          MaterializedView.builder().name("broken_mat_view").columns(List.of()).build();

      when(queryRunner.getMaterializedViewInfo("test_schema")).thenReturn(List.of(stubMatView));
      when(queryRunner.getColumnInfo("test_schema", "broken_mat_view"))
          .thenThrow(new SQLException("Column query timed out"));

      final SQLException exception =
          assertThrows(
              SQLException.class,
              () ->
                  viewBuilder.buildMaterializedViews(
                      queryRunner, "test_schema", Map.of(), Map.of()));

      assertEquals("Column query timed out", exception.getMessage());
    }
  }
}
