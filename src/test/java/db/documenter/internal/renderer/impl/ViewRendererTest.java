package db.documenter.internal.renderer.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Constraint;
import db.documenter.internal.models.db.View;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ViewRendererTest {

  private ViewRenderer viewRenderer;

  @BeforeEach
  void setUp() {
    viewRenderer = new ViewRenderer();
  }

  @Nested
  class RenderTests {

    @Test
    void rendersSimpleViewWithTwoColumns() {
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

      final var view =
          View.builder().name("active_users_view").columns(List.of(idColumn, nameColumn)).build();

      final var result = viewRenderer.render(view);

      final var expectedOutput =
          """
          \tentity "active_users_view" <<view>> {
          \t\tid: uuid
          \t\tname: text
          \t}
          """;

      assertEquals(expectedOutput, result);
    }

    @Test
    void rendersViewWithEmptyColumns() {
      final var view = View.builder().name("empty_view").columns(List.of()).build();

      final var result = viewRenderer.render(view);

      final var expectedOutput =
          """
          \tentity "empty_view" <<view>> {
          \t}
          """;

      assertEquals(expectedOutput, result);
    }

    @Test
    void rendersColumnWithMaximumLengthAsTypeWithLength() {
      final var emailColumn =
          Column.builder()
              .name("email")
              .dataType("varchar")
              .maximumLength(255)
              .constraints(List.of())
              .build();

      final var view = View.builder().name("user_view").columns(List.of(emailColumn)).build();

      final var result = viewRenderer.render(view);

      assertTrue(
          result.contains("email: varchar(255)"),
          "Column with maximumLength should append (length)");
    }

    @Test
    void rendersColumnWithZeroMaximumLengthWithoutLength() {
      final var idColumn =
          Column.builder()
              .name("id")
              .dataType("integer")
              .maximumLength(0)
              .constraints(List.of())
              .build();

      final var view = View.builder().name("order_view").columns(List.of(idColumn)).build();

      final var result = viewRenderer.render(view);

      assertTrue(
          result.contains("id: integer"),
          "Column with zero maximumLength should not append (length)");
      assertTrue(!result.contains("integer(0)"), "Column must not render (0) suffix");
    }

    @Test
    void rendersColumnWithSingleConstraintInAngleBrackets() {
      final var column =
          Column.builder()
              .name("email")
              .dataType("text")
              .maximumLength(0)
              .constraints(List.of(Constraint.UNIQUE))
              .build();

      final var view = View.builder().name("contact_view").columns(List.of(column)).build();

      final var result = viewRenderer.render(view);

      assertTrue(
          result.contains("email: text <<UNIQUE>>"),
          "Single constraint should be wrapped in <<...>>");
    }

    @Test
    void rendersColumnWithMultipleConstraintsSortedByDisplayPriority() {
      // NULLABLE has priority 5, UNIQUE has priority 1 — UNIQUE should appear first
      final var column =
          Column.builder()
              .name("code")
              .dataType("varchar")
              .maximumLength(10)
              .constraints(List.of(Constraint.NULLABLE, Constraint.UNIQUE))
              .build();

      final var view = View.builder().name("code_view").columns(List.of(column)).build();

      final var result = viewRenderer.render(view);

      assertTrue(
          result.contains("code: varchar(10) <<UNIQUE,NULLABLE>>"),
          "Constraints should be sorted by displayPriority: UNIQUE(1) before NULLABLE(5)");
    }

    @Test
    void rendersConstraintsInDisplayPriorityOrderNotDeclarationOrder() {
      // Declare in reverse priority order: GENERATED(6), DEFAULT(3), UNIQUE(1)
      // Expected output order: UNIQUE(1), DEFAULT(3), GENERATED(6)
      final var column =
          Column.builder()
              .name("status")
              .dataType("text")
              .maximumLength(0)
              .constraints(List.of(Constraint.GENERATED, Constraint.DEFAULT, Constraint.UNIQUE))
              .build();

      final var view = View.builder().name("status_view").columns(List.of(column)).build();

      final var result = viewRenderer.render(view);

      assertTrue(
          result.contains("<<UNIQUE,DEFAULT,GENERATED>>"),
          "Constraints must be sorted by displayPriority regardless of declaration order");
    }

    @Test
    void rendersColumnWithNoConstraintsWithoutAnnotation() {
      final var column =
          Column.builder()
              .name("created_at")
              .dataType("timestamp without time zone")
              .maximumLength(0)
              .constraints(List.of())
              .build();

      final var view = View.builder().name("audit_view").columns(List.of(column)).build();

      final var result = viewRenderer.render(view);

      // The column line itself should not contain a constraint annotation
      final var columnLine = "\t\tcreated_at: timestamp without time zone";
      assertTrue(
          result.contains(columnLine),
          "Column with no constraints should not have any <<...>> annotation");
      assertTrue(
          !result.contains(columnLine + " <<"),
          "Column line must not be followed by a constraint annotation");
    }

    @Test
    void usesViewStereotypeNotTableOrMaterializedViewStereotype() {
      final var column =
          Column.builder()
              .name("value")
              .dataType("text")
              .maximumLength(0)
              .constraints(List.of())
              .build();

      final var view = View.builder().name("test_view").columns(List.of(column)).build();

      final var result = viewRenderer.render(view);

      assertTrue(result.contains("<<view>>"), "Stereotype should be <<view>>");
      assertTrue(
          !result.contains("<<materialized_view>>"),
          "Stereotype must NOT be <<materialized_view>>");
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

      final var view = View.builder().name("no_pk_view").columns(List.of(column)).build();

      final var result = viewRenderer.render(view);

      assertTrue(
          !result.contains("**"), "Views should not use bold (**) formatting — no PK concept");
      assertTrue(!result.contains("--"), "Views should not use -- separator — no PK/non-PK split");
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

      final var view = View.builder().name("tab_view").columns(List.of(column)).build();

      final var result = viewRenderer.render(view);

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

      final var view =
          View.builder().name("ordered_view").columns(List.of(col1, col2, col3)).build();

      final var result = viewRenderer.render(view);

      final int zebraIndex = result.indexOf("zebra");
      final int appleIndex = result.indexOf("apple");
      final int mangoIndex = result.indexOf("mango");

      assertTrue(
          zebraIndex < appleIndex && appleIndex < mangoIndex,
          "Columns should be rendered in list order, not alphabetically");
    }

    @Test
    void rendersMultipleColumnsWithMixedConstraints() {
      final var idColumn =
          Column.builder()
              .name("id")
              .dataType("uuid")
              .maximumLength(0)
              .constraints(List.of())
              .build();
      final var nameColumn =
          Column.builder()
              .name("full_name")
              .dataType("varchar")
              .maximumLength(100)
              .constraints(List.of(Constraint.NULLABLE))
              .build();
      final var emailColumn =
          Column.builder()
              .name("email")
              .dataType("varchar")
              .maximumLength(255)
              .constraints(List.of(Constraint.UNIQUE, Constraint.NULLABLE))
              .build();

      final var view =
          View.builder()
              .name("user_profile_view")
              .columns(List.of(idColumn, nameColumn, emailColumn))
              .build();

      final var result = viewRenderer.render(view);

      final var expectedOutput =
          """
          \tentity "user_profile_view" <<view>> {
          \t\tid: uuid
          \t\tfull_name: varchar(100) <<NULLABLE>>
          \t\temail: varchar(255) <<UNIQUE,NULLABLE>>
          \t}
          """;

      assertEquals(expectedOutput, result);
    }

    @Test
    void rendersViewNameWithoutSchemaQualification() {
      final var column =
          Column.builder()
              .name("value")
              .dataType("text")
              .maximumLength(0)
              .constraints(List.of())
              .build();

      final var view = View.builder().name("sales_summary_view").columns(List.of(column)).build();

      final var result = viewRenderer.render(view);

      assertTrue(result.contains("\"sales_summary_view\""), "View name should appear in quotes");
      assertTrue(
          !result.contains("public.sales_summary_view"),
          "View name should not be schema-qualified");
    }

    @Test
    void rendersAllConstraintTypesCorrectly() {
      // FK(0), UNIQUE(1), AUTO_INCREMENT(2), DEFAULT(3), CHECK(4), NULLABLE(5), GENERATED(6)
      final var column =
          Column.builder()
              .name("col")
              .dataType("integer")
              .maximumLength(0)
              .constraints(
                  List.of(
                      Constraint.GENERATED,
                      Constraint.CHECK,
                      Constraint.NULLABLE,
                      Constraint.DEFAULT,
                      Constraint.AUTO_INCREMENT,
                      Constraint.UNIQUE,
                      Constraint.FK))
              .build();

      final var view = View.builder().name("all_constraints_view").columns(List.of(column)).build();

      final var result = viewRenderer.render(view);

      assertTrue(
          result.contains("<<FK,UNIQUE,AUTO_INCREMENT,DEFAULT,CHECK,NULLABLE,GENERATED>>"),
          "All constraints should appear sorted by displayPriority");
    }
  }
}
