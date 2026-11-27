package dev.shanelucy.dbdocumenter.internal.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.shanelucy.dbdocumenter.catalog.CatalogSnapshot;
import dev.shanelucy.dbdocumenter.catalog.Column;
import dev.shanelucy.dbdocumenter.catalog.DataType;
import dev.shanelucy.dbdocumenter.catalog.EnumType;
import dev.shanelucy.dbdocumenter.catalog.ForeignKey;
import dev.shanelucy.dbdocumenter.catalog.PrimaryKey;
import dev.shanelucy.dbdocumenter.catalog.QualifiedName;
import dev.shanelucy.dbdocumenter.catalog.ReferentialAction;
import dev.shanelucy.dbdocumenter.catalog.Schema;
import dev.shanelucy.dbdocumenter.catalog.Table;
import dev.shanelucy.dbdocumenter.catalog.UniqueKey;
import dev.shanelucy.dbdocumenter.internal.exceptions.ValidationException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CatalogDumpWriterTest {

  /** The {@code ## Example} of {@code docs/catalog-dump-format.md}, byte for byte. */
  private static final String SPEC_EXAMPLE =
      """
      schema auth
      table auth.user_mfa
      column auth.user_mfa.user_id type="int4" nullable=false
      column auth.user_mfa.method type="mfa_method" nullable=false user_type=auth.mfa_method
      pk auth.user_mfa(user_id, method) name=user_mfa_pkey
      fk auth.user_mfa(user_id) -> auth.users(id) on_delete=cascade on_update=no_action name=user_mfa_user_id_fkey
      table auth.users
      column auth.users.id type="int4" nullable=false
      column auth.users."Display Name" type="varchar(100)" nullable=true
      pk auth.users(id) name=users_pkey
      unique auth.users("Display Name")
      enum auth.mfa_method values=("totp", "sms")
      """;

  private static String dump(final Schema... schemas) {
    return CatalogDumpWriter.write(snapshot(schemas), CatalogDumpOptions.verbatim());
  }

  private static CatalogSnapshot snapshot(final Schema... schemas) {
    return CatalogSnapshot.builder()
        .productName("PostgreSQL")
        .productVersion("16.4")
        .schemas(List.of(schemas))
        .build();
  }

  private static Column column(final String name, final String type, final boolean nullable) {
    return Column.builder()
        .name(name)
        .nullable(nullable)
        .dataType(DataType.builder().displayText(type).build())
        .build();
  }

  private static Schema schemaWithTable(final String schemaName, final Table table) {
    return Schema.builder().name(schemaName).tables(List.of(table)).build();
  }

  private static Schema authSchema() {
    final var userMfa =
        Table.builder()
            .name(QualifiedName.of("auth", "user_mfa"))
            .columns(
                List.of(
                    column("user_id", "int4", false),
                    Column.builder()
                        .name("method")
                        .nullable(false)
                        .dataType(
                            DataType.builder()
                                .displayText("mfa_method")
                                .userType(QualifiedName.of("auth", "mfa_method"))
                                .build())
                        .build()))
            .primaryKey(
                PrimaryKey.builder()
                    .name("user_mfa_pkey")
                    .columns(List.of("user_id", "method"))
                    .build())
            .foreignKeys(
                List.of(
                    ForeignKey.builder()
                        .name("user_mfa_user_id_fkey")
                        .columns(List.of("user_id"))
                        .referencedTable(QualifiedName.of("auth", "users"))
                        .referencedColumns(List.of("id"))
                        .onDelete(ReferentialAction.CASCADE)
                        .onUpdate(ReferentialAction.NO_ACTION)
                        .build()))
            .build();
    final var users =
        Table.builder()
            .name(QualifiedName.of("auth", "users"))
            .columns(
                List.of(column("id", "int4", false), column("Display Name", "varchar(100)", true)))
            .primaryKey(PrimaryKey.builder().name("users_pkey").columns(List.of("id")).build())
            .uniqueKeys(List.of(UniqueKey.builder().columns(List.of("Display Name")).build()))
            .build();
    final var mfaMethod =
        EnumType.builder()
            .name(QualifiedName.of("auth", "mfa_method"))
            .values(List.of("totp", "sms"))
            .build();
    // Tables deliberately out of order: the model sorts them, the writer must not re-sort.
    return Schema.builder()
        .name("auth")
        .tables(List.of(users, userMfa))
        .enumTypes(List.of(mfaMethod))
        .build();
  }

  private static ForeignKey.Builder foreignKey() {
    return ForeignKey.builder()
        .columns(List.of("parent_id"))
        .referencedTable(QualifiedName.of("app", "parent"))
        .referencedColumns(List.of("id"));
  }

  private static String foreignKeyLine(final ForeignKey foreignKey) {
    final var table =
        Table.builder()
            .name(QualifiedName.of("app", "child"))
            .columns(List.of(column("parent_id", "int4", false)))
            .foreignKeys(List.of(foreignKey))
            .build();
    return dump(schemaWithTable("app", table)).lines().toList().getLast();
  }

  static Stream<Arguments> actionTokens() {
    return Stream.of(
        Arguments.of(ReferentialAction.NO_ACTION, "no_action"),
        Arguments.of(ReferentialAction.RESTRICT, "restrict"),
        Arguments.of(ReferentialAction.CASCADE, "cascade"),
        Arguments.of(ReferentialAction.SET_NULL, "set_null"),
        Arguments.of(ReferentialAction.SET_DEFAULT, "set_default"));
  }

  @Nested
  class SpecExampleTests {

    @Test
    void itWritesSpecExampleExactly() {
      assertEquals(SPEC_EXAMPLE, dump(authSchema()));
    }

    @Test
    void itEndsEveryLineWithLineFeedAndWritesNoCarriageReturn() {
      final var result = dump(authSchema());

      assertTrue(result.endsWith("\n"), "dump must end with a newline");
      assertFalse(result.contains("\r"), "dump must not contain a carriage return");
      assertFalse(result.contains("\n\n"), "dump must not contain a blank line");
      assertEquals(12, result.split("\n", -1).length - 1);
    }
  }

  @Nested
  class StructureTests {

    @Test
    void itWritesNothingWhenSnapshotHasNoSchemas() {
      assertEquals("", dump());
    }

    @Test
    void itWritesOnlySchemaLineWhenSchemaIsEmpty() {
      assertEquals("schema app\n", dump(Schema.builder().name("app").build()));
    }

    @Test
    void itWritesOnlyTableLineWhenTableHasNoColumnsOrKeys() {
      final var table = Table.builder().name(QualifiedName.of("app", "empty")).build();

      assertEquals("schema app\ntable app.empty\n", dump(schemaWithTable("app", table)));
    }

    @Test
    void itWritesSchemasInModelOrder() {
      final var result =
          dump(
              Schema.builder().name("zeta").build(),
              Schema.builder().name("Alpha").build(),
              Schema.builder().name("alpha").build());

      assertEquals("schema \"Alpha\"\nschema alpha\nschema zeta\n", result);
    }

    @Test
    void itWritesEnumsAfterAllTablesOfTheirSchema() {
      final var schema =
          Schema.builder()
              .name("app")
              .tables(List.of(Table.builder().name(QualifiedName.of("app", "zzz")).build()))
              .enumTypes(
                  List.of(
                      EnumType.builder()
                          .name(QualifiedName.of("app", "b_status"))
                          .values(List.of("on"))
                          .build(),
                      EnumType.builder()
                          .name(QualifiedName.of("app", "a_status"))
                          .values(List.of("off"))
                          .build()))
              .build();

      assertEquals(
          """
          schema app
          table app.zzz
          enum app.a_status values=("off")
          enum app.b_status values=("on")
          """,
          dump(schema));
    }

    @Test
    void itWritesEmptyEnumAsEmptyValueList() {
      final var schema =
          Schema.builder()
              .name("app")
              .enumTypes(List.of(EnumType.builder().name(QualifiedName.of("app", "e")).build()))
              .build();

      assertEquals("schema app\nenum app.e values=()\n", dump(schema));
    }

    @Test
    void itSeparatesMultipleColumnsWithCommaAndSpace() {
      final var table =
          Table.builder()
              .name(QualifiedName.of("app", "t"))
              .columns(List.of(column("a", "int4", false), column("b", "int4", false)))
              .primaryKey(PrimaryKey.builder().columns(List.of("a", "b")).build())
              .uniqueKeys(List.of(UniqueKey.builder().columns(List.of("b", "a")).build()))
              .foreignKeys(
                  List.of(
                      ForeignKey.builder()
                          .columns(List.of("a", "b"))
                          .referencedTable(QualifiedName.of("app", "p"))
                          .referencedColumns(List.of("x", "y"))
                          .build()))
              .build();

      assertEquals(
          """
          schema app
          table app.t
          column app.t.a type="int4" nullable=false
          column app.t.b type="int4" nullable=false
          pk app.t(a, b)
          unique app.t(b, a)
          fk app.t(a, b) -> app.p(x, y) on_delete=no_action on_update=no_action
          """,
          dump(schemaWithTable("app", table)));
    }
  }

  @Nested
  class OptionalAttributeTests {

    @Test
    void itOmitsNameAndUserTypeWhenAbsent() {
      final var table =
          Table.builder()
              .name(QualifiedName.of("app", "t"))
              .columns(List.of(column("id", "int4", true)))
              .primaryKey(PrimaryKey.builder().columns(List.of("id")).build())
              .uniqueKeys(List.of(UniqueKey.builder().columns(List.of("id")).build()))
              .foreignKeys(
                  List.of(
                      ForeignKey.builder()
                          .columns(List.of("id"))
                          .referencedTable(QualifiedName.of("app", "p"))
                          .referencedColumns(List.of("id"))
                          .build()))
              .build();

      assertEquals(
          """
          schema app
          table app.t
          column app.t.id type="int4" nullable=true
          pk app.t(id)
          unique app.t(id)
          fk app.t(id) -> app.p(id) on_delete=no_action on_update=no_action
          """,
          dump(schemaWithTable("app", table)));
    }

    @Test
    void itQuotesNamesThatAreNotBare() {
      final var table =
          Table.builder()
              .name(QualifiedName.of("app", "t"))
              .columns(List.of(column("id", "int4", false)))
              .primaryKey(PrimaryKey.builder().name("PK T").columns(List.of("id")).build())
              .uniqueKeys(List.of(UniqueKey.builder().name("uq.id").columns(List.of("id")).build()))
              .build();

      assertEquals(
          """
          schema app
          table app.t
          column app.t.id type="int4" nullable=false
          pk app.t(id) name="PK T"
          unique app.t(id) name="uq.id"
          """,
          dump(schemaWithTable("app", table)));
    }
  }

  @Nested
  class ReferentialActionTests {

    @ParameterizedTest
    @MethodSource("dev.shanelucy.dbdocumenter.internal.catalog.CatalogDumpWriterTest#actionTokens")
    void itWritesEveryOnDeleteAction(final ReferentialAction action, final String token) {
      final var line = foreignKeyLine(foreignKey().onDelete(action).build());

      assertEquals(
          "fk app.child(parent_id) -> app.parent(id) on_delete=" + token + " on_update=no_action",
          line);
    }

    @ParameterizedTest
    @MethodSource("dev.shanelucy.dbdocumenter.internal.catalog.CatalogDumpWriterTest#actionTokens")
    void itWritesEveryOnUpdateAction(final ReferentialAction action, final String token) {
      final var line = foreignKeyLine(foreignKey().onUpdate(action).build());

      assertEquals(
          "fk app.child(parent_id) -> app.parent(id) on_delete=no_action on_update=" + token, line);
    }
  }

  @Nested
  class QuotingTests {

    @Test
    void itQuotesMixedCaseSpacesAndDotsInIdentifiers() {
      final var table =
          Table.builder()
              .name(QualifiedName.of("My.Schema", "Order Items"))
              .columns(List.of(column("Line.No", "int4", false)))
              .primaryKey(PrimaryKey.builder().columns(List.of("Line.No")).build())
              .build();

      assertEquals(
          """
          schema "My.Schema"
          table "My.Schema"."Order Items"
          column "My.Schema"."Order Items"."Line.No" type="int4" nullable=false
          pk "My.Schema"."Order Items"("Line.No")
          """,
          dump(schemaWithTable("My.Schema", table)));
    }

    @Test
    void itQuotesEmptyTypeDisplayText() {
      final var table =
          Table.builder()
              .name(QualifiedName.of("main", "t"))
              .columns(List.of(column("c", "", true)))
              .build();

      assertEquals(
          "column main.t.c type=\"\" nullable=true",
          dump(schemaWithTable("main", table)).lines().toList().getLast());
    }

    @Test
    void itEscapesSpecialCharactersInTypeDisplayText() {
      final var table =
          Table.builder()
              .name(QualifiedName.of("main", "t"))
              .columns(List.of(column("c", "enum(\"a\",\t'b\\c')\r\n", true)))
              .build();

      assertEquals(
          "column main.t.c type=\"enum(\\\"a\\\",\\t'b\\\\c')\\r\\n\" nullable=true",
          dump(schemaWithTable("main", table)).lines().toList().getLast());
    }

    @Test
    void itEscapesSpecialAndNonPrintableCharactersInEnumLabels() {
      final var schema =
          Schema.builder()
              .name("app")
              .enumTypes(
                  List.of(
                      EnumType.builder()
                          .name(QualifiedName.of("app", "Mood"))
                          .values(
                              List.of(
                                  "line\nbreak",
                                  "tab\there",
                                  "say \"hi\"",
                                  "back\\slash",
                                  "bell\u0007",
                                  ""))
                          .build()))
              .build();

      final var result = dump(schema);

      assertEquals(
          "schema app\n"
              + "enum app.\"Mood\" values=(\"line\\nbreak\", \"tab\\there\", \"say \\\"hi\\\"\","
              + " \"back\\\\slash\", \"bell\\u0007\", \"\")\n",
          result);
    }

    @Test
    void itQuotesReferencedQualifiedNameAndUserType() {
      final var table =
          Table.builder()
              .name(QualifiedName.of("app", "child"))
              .columns(
                  List.of(
                      Column.builder()
                          .name("parent_id")
                          .dataType(
                              DataType.builder()
                                  .displayText("\"Other\".\"Id Type\"")
                                  .userType(QualifiedName.of("Other", "Id Type"))
                                  .build())
                          .build()))
              .foreignKeys(
                  List.of(
                      ForeignKey.builder()
                          .name("Child Parent FK")
                          .columns(List.of("parent_id"))
                          .referencedTable(QualifiedName.of("Other", "Parent Table"))
                          .referencedColumns(List.of("Id"))
                          .onDelete(ReferentialAction.SET_NULL)
                          .build()))
              .build();

      assertEquals(
          """
          schema app
          table app.child
          column app.child.parent_id type="\\"Other\\".\\"Id Type\\"" nullable=true \
          user_type="Other"."Id Type"
          fk app.child(parent_id) -> "Other"."Parent Table"("Id") \
          on_delete=set_null on_update=no_action name="Child Parent FK"
          """,
          dump(schemaWithTable("app", table)));
    }
  }

  @Nested
  class ValidationTests {

    @Test
    void itThrowsExceptionWhenSnapshotIsNull() {
      final var options = CatalogDumpOptions.verbatim();

      final var exception =
          assertThrows(ValidationException.class, () -> CatalogDumpWriter.write(null, options));

      assertEquals("snapshot must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenOptionsIsNull() {
      final var snapshot = snapshot();

      final var exception =
          assertThrows(ValidationException.class, () -> CatalogDumpWriter.write(snapshot, null));

      assertEquals("options must not be null", exception.getMessage());
    }
  }
}
