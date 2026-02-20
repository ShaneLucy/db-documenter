package db.documenter.internal.renderer.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Constraint;
import db.documenter.internal.models.db.MaterializedView;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MaterializedViewRendererTest {

  private MaterializedViewRenderer materializedViewRenderer;

  @BeforeEach
  void setUp() {
    materializedViewRenderer = new MaterializedViewRenderer();
  }

  @Nested
  class RenderTests {

    @Test
    void rendersSimpleMaterializedViewWithTwoColumns() {
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

      final var matView =
          MaterializedView.builder()
              .name("monthly_sales_summary")
              .columns(List.of(monthColumn, revenueColumn))
              .build();

      final var result = materializedViewRenderer.render(matView);

      final var expectedOutput =
          """
          \tentity "monthly_sales_summary" <<materialized_view>> {
          \t\tmonth: date
          \t\ttotal_revenue: numeric
          \t}
          """;

      assertEquals(expectedOutput, result);
    }

    @Test
    void rendersMaterializedViewWithEmptyColumns() {
      final var matView =
          MaterializedView.builder().name("empty_mat_view").columns(List.of()).build();

      final var result = materializedViewRenderer.render(matView);

      final var expectedOutput =
          """
          \tentity "empty_mat_view" <<materialized_view>> {
          \t}
          """;

      assertEquals(expectedOutput, result);
    }

    @Test
    void rendersColumnWithMaximumLengthAsTypeWithLength() {
      final var descriptionColumn =
          Column.builder()
              .name("description")
              .dataType("character varying")
              .maximumLength(500)
              .constraints(List.of())
              .build();

      final var matView =
          MaterializedView.builder()
              .name("product_summary")
              .columns(List.of(descriptionColumn))
              .build();

      final var result = materializedViewRenderer.render(matView);

      assertTrue(
          result.contains("description: character varying(500)"),
          "Column with maximumLength should append (length)");
    }

    @Test
    void rendersColumnWithZeroMaximumLengthWithoutLength() {
      final var countColumn =
          Column.builder()
              .name("order_count")
              .dataType("integer")
              .maximumLength(0)
              .constraints(List.of())
              .build();

      final var matView =
          MaterializedView.builder().name("order_summary").columns(List.of(countColumn)).build();

      final var result = materializedViewRenderer.render(matView);

      assertTrue(
          result.contains("order_count: integer"),
          "Column with zero maximumLength should not append (length)");
      assertTrue(!result.contains("integer(0)"), "Column must not render (0) suffix");
    }

    @Test
    void rendersColumnWithSingleConstraintInAngleBrackets() {
      final var column =
          Column.builder()
              .name("product_code")
              .dataType("text")
              .maximumLength(0)
              .constraints(List.of(Constraint.UNIQUE))
              .build();

      final var matView =
          MaterializedView.builder().name("product_catalog").columns(List.of(column)).build();

      final var result = materializedViewRenderer.render(matView);

      assertTrue(
          result.contains("product_code: text <<UNIQUE>>"),
          "Single constraint should be wrapped in <<...>>");
    }

    @Test
    void rendersColumnWithMultipleConstraintsSortedByDisplayPriority() {
      // NULLABLE has priority 5, DEFAULT has priority 3 — DEFAULT should appear first
      final var column =
          Column.builder()
              .name("total")
              .dataType("numeric")
              .maximumLength(0)
              .constraints(List.of(Constraint.NULLABLE, Constraint.DEFAULT))
              .build();

      final var matView =
          MaterializedView.builder().name("totals_view").columns(List.of(column)).build();

      final var result = materializedViewRenderer.render(matView);

      assertTrue(
          result.contains("total: numeric <<DEFAULT,NULLABLE>>"),
          "Constraints should be sorted by displayPriority: DEFAULT(3) before NULLABLE(5)");
    }

    @Test
    void rendersConstraintsInDisplayPriorityOrderNotDeclarationOrder() {
      // Declare in reverse priority order: GENERATED(6), AUTO_INCREMENT(2), FK(0)
      // Expected output order: FK(0), AUTO_INCREMENT(2), GENERATED(6)
      final var column =
          Column.builder()
              .name("id")
              .dataType("bigint")
              .maximumLength(0)
              .constraints(List.of(Constraint.GENERATED, Constraint.AUTO_INCREMENT, Constraint.FK))
              .build();

      final var matView =
          MaterializedView.builder().name("id_view").columns(List.of(column)).build();

      final var result = materializedViewRenderer.render(matView);

      assertTrue(
          result.contains("<<FK,AUTO_INCREMENT,GENERATED>>"),
          "Constraints must be sorted by displayPriority regardless of declaration order");
    }

    @Test
    void rendersColumnWithNoConstraintsWithoutAnnotation() {
      final var column =
          Column.builder()
              .name("refreshed_at")
              .dataType("timestamp without time zone")
              .maximumLength(0)
              .constraints(List.of())
              .build();

      final var matView =
          MaterializedView.builder().name("refresh_log").columns(List.of(column)).build();

      final var result = materializedViewRenderer.render(matView);

      // The column line itself should not contain a constraint annotation
      final var columnLine = "\t\trefreshed_at: timestamp without time zone";
      assertTrue(
          result.contains(columnLine),
          "Column with no constraints should not have any <<...>> annotation");
      assertTrue(
          !result.contains(columnLine + " <<"),
          "Column line must not be followed by a constraint annotation");
    }

    @Test
    void usesMaterializedViewStereotypeNotViewOrTableStereotype() {
      final var column =
          Column.builder()
              .name("value")
              .dataType("text")
              .maximumLength(0)
              .constraints(List.of())
              .build();

      final var matView =
          MaterializedView.builder().name("test_mat_view").columns(List.of(column)).build();

      final var result = materializedViewRenderer.render(matView);

      assertTrue(
          result.contains("<<materialized_view>>"), "Stereotype should be <<materialized_view>>");
      assertTrue(!result.contains("<<view>>"), "Stereotype must NOT be <<view>>");
      assertTrue(!result.contains("<<table>>"), "Stereotype must NOT be <<table>>");
    }

    @Test
    void doesNotRenderBoldFormattingOrSeparatorLine() {
      final var column =
          Column.builder()
              .name("id")
              .dataType("uuid")
              .maximumLength(0)
              .constraints(List.of())
              .build();

      final var matView =
          MaterializedView.builder().name("no_pk_mat_view").columns(List.of(column)).build();

      final var result = materializedViewRenderer.render(matView);

      assertTrue(
          !result.contains("**"),
          "Materialized views should not use bold (**) formatting — no PK concept");
      assertTrue(
          !result.contains("--"),
          "Materialized views should not use -- separator — no PK/non-PK split");
    }

    @Test
    void usesProperIndentationWithTabs() {
      final var column =
          Column.builder()
              .name("col")
              .dataType("integer")
              .maximumLength(0)
              .constraints(List.of())
              .build();

      final var matView =
          MaterializedView.builder().name("tab_mat_view").columns(List.of(column)).build();

      final var result = materializedViewRenderer.render(matView);

      assertTrue(
          result.startsWith("\tentity"), "Entity declaration should start with a single tab");
      assertTrue(result.contains("\t\tcol: integer"), "Columns should start with two tabs");
      assertTrue(result.contains("\t}\n"), "Closing brace should start with a single tab");
    }

    @Test
    void preservesColumnOrderFromList() {
      final var col1 =
          Column.builder()
              .name("zebra")
              .dataType("text")
              .maximumLength(0)
              .constraints(List.of())
              .build();
      final var col2 =
          Column.builder()
              .name("apple")
              .dataType("text")
              .maximumLength(0)
              .constraints(List.of())
              .build();
      final var col3 =
          Column.builder()
              .name("mango")
              .dataType("text")
              .maximumLength(0)
              .constraints(List.of())
              .build();

      final var matView =
          MaterializedView.builder()
              .name("ordered_mat_view")
              .columns(List.of(col1, col2, col3))
              .build();

      final var result = materializedViewRenderer.render(matView);

      final int zebraIndex = result.indexOf("zebra");
      final int appleIndex = result.indexOf("apple");
      final int mangoIndex = result.indexOf("mango");

      assertTrue(
          zebraIndex < appleIndex && appleIndex < mangoIndex,
          "Columns should be rendered in list order, not alphabetically");
    }

    @Test
    void rendersMultipleColumnsWithMixedConstraints() {
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
              .constraints(List.of(Constraint.NULLABLE))
              .build();
      final var countColumn =
          Column.builder()
              .name("order_count")
              .dataType("integer")
              .maximumLength(0)
              .constraints(List.of(Constraint.DEFAULT, Constraint.NULLABLE))
              .build();

      final var matView =
          MaterializedView.builder()
              .name("sales_mat_view")
              .columns(List.of(monthColumn, revenueColumn, countColumn))
              .build();

      final var result = materializedViewRenderer.render(matView);

      final var expectedOutput =
          """
          \tentity "sales_mat_view" <<materialized_view>> {
          \t\tmonth: date
          \t\ttotal_revenue: numeric <<NULLABLE>>
          \t\torder_count: integer <<DEFAULT,NULLABLE>>
          \t}
          """;

      assertEquals(expectedOutput, result);
    }

    @Test
    void rendersMaterializedViewNameWithoutSchemaQualification() {
      final var column =
          Column.builder()
              .name("value")
              .dataType("text")
              .maximumLength(0)
              .constraints(List.of())
              .build();

      final var matView =
          MaterializedView.builder().name("revenue_by_region").columns(List.of(column)).build();

      final var result = materializedViewRenderer.render(matView);

      assertTrue(
          result.contains("\"revenue_by_region\""),
          "Materialized view name should appear in quotes");
      assertTrue(
          !result.contains("public.revenue_by_region"),
          "Materialized view name should not be schema-qualified");
    }
  }
}
