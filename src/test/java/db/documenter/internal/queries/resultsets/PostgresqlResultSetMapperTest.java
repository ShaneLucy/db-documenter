package db.documenter.internal.queries.resultsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.PrimaryKey;
import db.documenter.internal.queries.impl.postgresql.resultsets.PostgresqlResultSetMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostgresqlResultSetMapperTest {

  @Mock private ResultSet resultSet;

  private PostgresqlResultSetMapper postgresqlResultSetMapper;

  @BeforeEach
  void setUp() {
    Mockito.reset(resultSet);
    postgresqlResultSetMapper = new PostgresqlResultSetMapper();
  }

  @Nested
  class MapToTablesTests {
    private String tableNameValue;

    @BeforeEach
    void setUp() {
      tableNameValue = "some table name";
    }

    @Test
    void itMapsItemInResultSetCorrectly() throws SQLException {
      when(resultSet.next()).thenReturn(true, false);
      when(resultSet.getString("table_name")).thenReturn(tableNameValue);

      final var result = postgresqlResultSetMapper.mapToTables(resultSet);

      assertEquals(1, result.size());

      assertEquals(tableNameValue, result.getFirst().name());

      verifyNoMoreInteractions(resultSet);
    }

    @Test
    void itMapsAllItemsInResultSetCorrectly() throws SQLException {
      final List<String> tableNames = List.of(tableNameValue, "second name value");

      final Iterator<String> namesIt = tableNames.iterator();

      when(resultSet.next()).thenReturn(true, true, false);
      when(resultSet.getString("table_name")).thenAnswer(invocation -> namesIt.next());

      final var result = postgresqlResultSetMapper.mapToTables(resultSet);

      assertEquals(2, result.size());
      assertEquals(tableNameValue, result.getFirst().name());

      assertEquals("second name value", result.get(1).name());
      verifyNoMoreInteractions(resultSet);
    }

    @Test
    void ifResultSetIsEmptyItReturnsEmptyArray() throws SQLException {
      when(resultSet.next()).thenReturn(false);

      final var result = postgresqlResultSetMapper.mapToTables(resultSet);

      assertTrue(result.isEmpty());
      verifyNoMoreInteractions(resultSet);
    }
  }

  @Nested
  class MapToColumnsTests {
    private String colName1;
    private String colName2;
    private int ordinal1;
    private int ordinal2;
    private String nullable1;
    private String nullable2;
    private String dataType1;
    private String dataType2;
    private int maxLength1;
    private int maxLength2;

    @BeforeEach
    void setUp() {
      colName1 = "id";
      colName2 = "name";
      ordinal1 = 1;
      ordinal2 = 2;
      nullable1 = "NO";
      nullable2 = "YES";
      dataType1 = "int";
      dataType2 = "varchar";
      maxLength1 = 10;
      maxLength2 = 50;
    }

    @Test
    void itMapsSingleColumnCorrectly() throws SQLException {
      when(resultSet.next()).thenReturn(true, false);

      when(resultSet.getString("column_name")).thenReturn(colName1);
      when(resultSet.getInt("ordinal_position")).thenReturn(ordinal1);
      when(resultSet.getString("is_nullable")).thenReturn(nullable1);
      when(resultSet.getString("data_type")).thenReturn(dataType1);
      when(resultSet.getInt("character_maximum_length")).thenReturn(maxLength1);
      when(resultSet.getBoolean("is_unique")).thenReturn(false);
      when(resultSet.getString("check_constraint")).thenReturn(null);
      when(resultSet.getString("column_default")).thenReturn(null);
      when(resultSet.getBoolean("is_auto_increment")).thenReturn(false);

      final List<Column> result = postgresqlResultSetMapper.mapToColumns(resultSet);

      assertEquals(1, result.size());
      final var col = result.getFirst();
      assertEquals(colName1, col.name());
      assertEquals(ordinal1, col.ordinalPosition());
      assertFalse(col.isNullable());
      assertEquals(dataType1, col.dataType());
      assertEquals(maxLength1, col.maximumLength());
      verifyNoMoreInteractions(resultSet);
    }

    @Test
    void itMapsMultipleColumnsCorrectly() throws SQLException {
      when(resultSet.next()).thenReturn(true, true, false);

      final List<String> names = List.of(colName1, colName2);
      final List<Integer> ordinals = List.of(ordinal1, ordinal2);
      final List<String> nullables = List.of(nullable1, nullable2);
      final List<String> dataTypes = List.of(dataType1, dataType2);
      final List<Integer> maxLengths = List.of(maxLength1, maxLength2);
      final List<Boolean> isUniques = List.of(false, false);
      final List<String> checkConstraints = java.util.Arrays.asList(null, null);
      final List<String> columnDefaults = java.util.Arrays.asList(null, null);
      final List<Boolean> isAutoIncrements = List.of(false, false);

      final Iterator<String> nameIt = names.iterator();
      final Iterator<Integer> ordinalIt = ordinals.iterator();
      final Iterator<String> nullableIt = nullables.iterator();
      final Iterator<String> typeIt = dataTypes.iterator();
      final Iterator<Integer> lengthIt = maxLengths.iterator();
      final Iterator<Boolean> isUniqueIt = isUniques.iterator();
      final Iterator<String> checkConstraintIt = checkConstraints.iterator();
      final Iterator<String> columnDefaultIt = columnDefaults.iterator();
      final Iterator<Boolean> isAutoIncrementIt = isAutoIncrements.iterator();

      when(resultSet.getString("column_name")).thenAnswer(invocation -> nameIt.next());
      when(resultSet.getInt("ordinal_position")).thenAnswer(invocation -> ordinalIt.next());
      when(resultSet.getString("is_nullable")).thenAnswer(invocation -> nullableIt.next());
      when(resultSet.getString("data_type")).thenAnswer(invocation -> typeIt.next());
      when(resultSet.getInt("character_maximum_length")).thenAnswer(invocation -> lengthIt.next());
      when(resultSet.getBoolean("is_unique")).thenAnswer(invocation -> isUniqueIt.next());
      when(resultSet.getString("check_constraint"))
          .thenAnswer(invocation -> checkConstraintIt.next());
      when(resultSet.getString("column_default")).thenAnswer(invocation -> columnDefaultIt.next());
      when(resultSet.getBoolean("is_auto_increment"))
          .thenAnswer(invocation -> isAutoIncrementIt.next());

      final List<Column> result = postgresqlResultSetMapper.mapToColumns(resultSet);

      assertEquals(2, result.size());

      final var first = result.getFirst();
      assertEquals(colName1, first.name());
      assertEquals(ordinal1, first.ordinalPosition());
      assertFalse(first.isNullable());
      assertEquals(dataType1, first.dataType());
      assertEquals(maxLength1, first.maximumLength());

      final var second = result.get(1);
      assertEquals(colName2, second.name());
      assertEquals(ordinal2, second.ordinalPosition());
      assertTrue(second.isNullable());
      assertEquals(dataType2, second.dataType());
      assertEquals(maxLength2, second.maximumLength());
      verifyNoMoreInteractions(resultSet);
    }

    @Test
    void ifResultSetIsEmptyItReturnsEmptyList() throws SQLException {
      when(resultSet.next()).thenReturn(false);

      final List<Column> result = postgresqlResultSetMapper.mapToColumns(resultSet);

      assertTrue(result.isEmpty());
      verifyNoMoreInteractions(resultSet);
    }

    @Test
    void addsNullableConstraintWhenIsNullableIsYes() throws SQLException {
      when(resultSet.next()).thenReturn(true, false);
      when(resultSet.getString("column_name")).thenReturn("name");
      when(resultSet.getInt("ordinal_position")).thenReturn(1);
      when(resultSet.getString("is_nullable")).thenReturn("YES");
      when(resultSet.getString("data_type")).thenReturn("varchar");
      when(resultSet.getInt("character_maximum_length")).thenReturn(50);
      when(resultSet.getBoolean("is_unique")).thenReturn(false);
      when(resultSet.getString("check_constraint")).thenReturn(null);
      when(resultSet.getString("column_default")).thenReturn(null);
      when(resultSet.getBoolean("is_auto_increment")).thenReturn(false);

      final List<Column> result = postgresqlResultSetMapper.mapToColumns(resultSet);

      assertEquals(1, result.size());
      final var col = result.getFirst();
      assertTrue(col.isNullable());
      assertTrue(col.constraints().contains(db.documenter.internal.models.db.Constraint.NULLABLE));
    }

    @Test
    void doesNotAddNullableConstraintWhenIsNullableIsNo() throws SQLException {
      when(resultSet.next()).thenReturn(true, false);
      when(resultSet.getString("column_name")).thenReturn("id");
      when(resultSet.getInt("ordinal_position")).thenReturn(1);
      when(resultSet.getString("is_nullable")).thenReturn("NO");
      when(resultSet.getString("data_type")).thenReturn("int");
      when(resultSet.getInt("character_maximum_length")).thenReturn(10);
      when(resultSet.getBoolean("is_unique")).thenReturn(false);
      when(resultSet.getString("check_constraint")).thenReturn(null);
      when(resultSet.getString("column_default")).thenReturn(null);
      when(resultSet.getBoolean("is_auto_increment")).thenReturn(false);

      final List<Column> result = postgresqlResultSetMapper.mapToColumns(resultSet);

      assertEquals(1, result.size());
      final var col = result.getFirst();
      assertFalse(col.isNullable());
      assertFalse(col.constraints().contains(db.documenter.internal.models.db.Constraint.NULLABLE));
    }

    @Test
    void addsUniqueConstraintWhenIsUniqueIsTrue() throws SQLException {
      when(resultSet.next()).thenReturn(true, false);
      when(resultSet.getString("column_name")).thenReturn("email");
      when(resultSet.getInt("ordinal_position")).thenReturn(1);
      when(resultSet.getString("is_nullable")).thenReturn("NO");
      when(resultSet.getString("data_type")).thenReturn("varchar");
      when(resultSet.getInt("character_maximum_length")).thenReturn(100);
      when(resultSet.getBoolean("is_unique")).thenReturn(true);
      when(resultSet.getString("check_constraint")).thenReturn(null);
      when(resultSet.getString("column_default")).thenReturn(null);
      when(resultSet.getBoolean("is_auto_increment")).thenReturn(false);

      final List<Column> result = postgresqlResultSetMapper.mapToColumns(resultSet);

      assertEquals(1, result.size());
      assertTrue(
          result
              .getFirst()
              .constraints()
              .contains(db.documenter.internal.models.db.Constraint.UNIQUE));
    }

    @Test
    void addsCheckConstraintWhenCheckConstraintIsNotBlank() throws SQLException {
      when(resultSet.next()).thenReturn(true, false);
      when(resultSet.getString("column_name")).thenReturn("age");
      when(resultSet.getInt("ordinal_position")).thenReturn(1);
      when(resultSet.getString("is_nullable")).thenReturn("NO");
      when(resultSet.getString("data_type")).thenReturn("int");
      when(resultSet.getInt("character_maximum_length")).thenReturn(0);
      when(resultSet.getBoolean("is_unique")).thenReturn(false);
      when(resultSet.getString("check_constraint")).thenReturn("age > 0");
      when(resultSet.getString("column_default")).thenReturn(null);
      when(resultSet.getBoolean("is_auto_increment")).thenReturn(false);

      final List<Column> result = postgresqlResultSetMapper.mapToColumns(resultSet);

      assertEquals(1, result.size());
      assertTrue(
          result
              .getFirst()
              .constraints()
              .contains(db.documenter.internal.models.db.Constraint.CHECK));
    }

    @Test
    void doesNotAddCheckConstraintWhenCheckConstraintIsBlank() throws SQLException {
      when(resultSet.next()).thenReturn(true, false);
      when(resultSet.getString("column_name")).thenReturn("age");
      when(resultSet.getInt("ordinal_position")).thenReturn(1);
      when(resultSet.getString("is_nullable")).thenReturn("NO");
      when(resultSet.getString("data_type")).thenReturn("int");
      when(resultSet.getInt("character_maximum_length")).thenReturn(0);
      when(resultSet.getBoolean("is_unique")).thenReturn(false);
      when(resultSet.getString("check_constraint")).thenReturn("   ");
      when(resultSet.getString("column_default")).thenReturn(null);
      when(resultSet.getBoolean("is_auto_increment")).thenReturn(false);

      final List<Column> result = postgresqlResultSetMapper.mapToColumns(resultSet);

      assertEquals(1, result.size());
      assertFalse(
          result
              .getFirst()
              .constraints()
              .contains(db.documenter.internal.models.db.Constraint.CHECK));
    }

    @Test
    void addsDefaultConstraintWhenColumnDefaultIsNotBlank() throws SQLException {
      when(resultSet.next()).thenReturn(true, false);
      when(resultSet.getString("column_name")).thenReturn("status");
      when(resultSet.getInt("ordinal_position")).thenReturn(1);
      when(resultSet.getString("is_nullable")).thenReturn("NO");
      when(resultSet.getString("data_type")).thenReturn("varchar");
      when(resultSet.getInt("character_maximum_length")).thenReturn(50);
      when(resultSet.getBoolean("is_unique")).thenReturn(false);
      when(resultSet.getString("check_constraint")).thenReturn(null);
      when(resultSet.getString("column_default")).thenReturn("'active'");
      when(resultSet.getBoolean("is_auto_increment")).thenReturn(false);

      final List<Column> result = postgresqlResultSetMapper.mapToColumns(resultSet);

      assertEquals(1, result.size());
      assertTrue(
          result
              .getFirst()
              .constraints()
              .contains(db.documenter.internal.models.db.Constraint.DEFAULT));
    }

    @Test
    void doesNotAddDefaultConstraintWhenColumnDefaultIsBlank() throws SQLException {
      when(resultSet.next()).thenReturn(true, false);
      when(resultSet.getString("column_name")).thenReturn("status");
      when(resultSet.getInt("ordinal_position")).thenReturn(1);
      when(resultSet.getString("is_nullable")).thenReturn("NO");
      when(resultSet.getString("data_type")).thenReturn("varchar");
      when(resultSet.getInt("character_maximum_length")).thenReturn(50);
      when(resultSet.getBoolean("is_unique")).thenReturn(false);
      when(resultSet.getString("check_constraint")).thenReturn(null);
      when(resultSet.getString("column_default")).thenReturn("  ");
      when(resultSet.getBoolean("is_auto_increment")).thenReturn(false);

      final List<Column> result = postgresqlResultSetMapper.mapToColumns(resultSet);

      assertEquals(1, result.size());
      assertFalse(
          result
              .getFirst()
              .constraints()
              .contains(db.documenter.internal.models.db.Constraint.DEFAULT));
    }

    @Test
    void addsAutoIncrementConstraintWhenIsAutoIncrementIsTrue() throws SQLException {
      when(resultSet.next()).thenReturn(true, false);
      when(resultSet.getString("column_name")).thenReturn("id");
      when(resultSet.getInt("ordinal_position")).thenReturn(1);
      when(resultSet.getString("is_nullable")).thenReturn("NO");
      when(resultSet.getString("data_type")).thenReturn("int");
      when(resultSet.getInt("character_maximum_length")).thenReturn(0);
      when(resultSet.getBoolean("is_unique")).thenReturn(false);
      when(resultSet.getString("check_constraint")).thenReturn(null);
      when(resultSet.getString("column_default")).thenReturn(null);
      when(resultSet.getBoolean("is_auto_increment")).thenReturn(true);

      final List<Column> result = postgresqlResultSetMapper.mapToColumns(resultSet);

      assertEquals(1, result.size());
      assertTrue(
          result
              .getFirst()
              .constraints()
              .contains(db.documenter.internal.models.db.Constraint.AUTO_INCREMENT));
    }

    @Test
    void addsMultipleConstraintsWhenApplicable() throws SQLException {
      when(resultSet.next()).thenReturn(true, false);
      when(resultSet.getString("column_name")).thenReturn("email");
      when(resultSet.getInt("ordinal_position")).thenReturn(1);
      when(resultSet.getString("is_nullable")).thenReturn("YES");
      when(resultSet.getString("data_type")).thenReturn("varchar");
      when(resultSet.getInt("character_maximum_length")).thenReturn(100);
      when(resultSet.getBoolean("is_unique")).thenReturn(true);
      when(resultSet.getString("check_constraint")).thenReturn("email LIKE '%@%'");
      when(resultSet.getString("column_default")).thenReturn("'user@example.com'");
      when(resultSet.getBoolean("is_auto_increment")).thenReturn(false);

      final List<Column> result = postgresqlResultSetMapper.mapToColumns(resultSet);

      assertEquals(1, result.size());
      final var col = result.getFirst();
      assertTrue(col.constraints().contains(db.documenter.internal.models.db.Constraint.UNIQUE));
      assertTrue(col.constraints().contains(db.documenter.internal.models.db.Constraint.CHECK));
      assertTrue(col.constraints().contains(db.documenter.internal.models.db.Constraint.DEFAULT));
      assertTrue(col.constraints().contains(db.documenter.internal.models.db.Constraint.NULLABLE));
      assertFalse(
          col.constraints().contains(db.documenter.internal.models.db.Constraint.AUTO_INCREMENT));
    }
  }

  @Nested
  class MapToPrimaryKeyTests {

    private final String constraintName1 = "pk_user_id";
    private final String column1 = "id";
    private final String column2 = "username";

    @Test
    void itReturnsNullIfResultSetIsEmpty() throws SQLException {
      when(resultSet.next()).thenReturn(false);

      final PrimaryKey result = postgresqlResultSetMapper.mapToPrimaryKey(resultSet);

      assertNull(result);
    }

    @Test
    void itMapsSingleColumnPrimaryKeyCorrectly() throws SQLException {
      when(resultSet.next()).thenReturn(true, false);
      when(resultSet.getString("constraint_name")).thenReturn(constraintName1);
      when(resultSet.getString("column_name")).thenReturn(column1);

      final PrimaryKey result = postgresqlResultSetMapper.mapToPrimaryKey(resultSet);

      assertEquals(constraintName1, result.constraintName());
      assertEquals(List.of(column1), result.columnNames());
    }

    @Test
    void itMapsMultiColumnPrimaryKeyCorrectly() throws SQLException {
      when(resultSet.next()).thenReturn(true, true, false);

      final List<String> columnNames = List.of(column1, column2);
      final Iterator<String> columnIt = columnNames.iterator();

      when(resultSet.getString("constraint_name")).thenReturn(constraintName1);
      when(resultSet.getString("column_name")).thenAnswer(invocation -> columnIt.next());

      final PrimaryKey result = postgresqlResultSetMapper.mapToPrimaryKey(resultSet);

      assertEquals(constraintName1, result.constraintName());
      assertEquals(columnNames, result.columnNames());
    }
  }

  @Nested
  class MapToForeignKeysTests {

    private final String fkName1 = "fk_user_id";
    private final String fkName2 = "fk_order_id";
    private final String sourceTable1 = "users";
    private final String sourceTable2 = "orders";
    private final String sourceColumn1 = "id";
    private final String sourceColumn2 = "order_id";
    private final String targetTable1 = "accounts";
    private final String targetTable2 = "customers";
    private final String targetColumn1 = "account_id";
    private final String targetColumn2 = "customer_id";

    @Test
    void itMapsSingleForeignKeyCorrectly() throws SQLException {
      when(resultSet.next()).thenReturn(true, false);

      when(resultSet.getString("constraint_name")).thenReturn(fkName1);
      when(resultSet.getString("source_table_name")).thenReturn(sourceTable1);
      when(resultSet.getString("source_column")).thenReturn(sourceColumn1);
      when(resultSet.getString("referenced_table")).thenReturn(targetTable1);
      when(resultSet.getString("referenced_column")).thenReturn(targetColumn1);

      final List<ForeignKey> result = postgresqlResultSetMapper.mapToForeignKeys(resultSet);

      assertEquals(1, result.size());
      final ForeignKey foreignKey = result.getFirst();
      assertEquals(fkName1, foreignKey.name());
      assertEquals(sourceTable1, foreignKey.sourceTable());
      assertEquals(sourceColumn1, foreignKey.sourceColumn());
      assertEquals(targetTable1, foreignKey.targetTable());
      assertEquals(targetColumn1, foreignKey.targetColumn());
    }

    @Test
    void itMapsMultipleForeignKeysCorrectly() throws SQLException {
      when(resultSet.next()).thenReturn(true, true, false);

      final List<String> fkNames = List.of(fkName1, fkName2);
      final List<String> sourceTables = List.of(sourceTable1, sourceTable2);
      final List<String> sourceColumns = List.of(sourceColumn1, sourceColumn2);
      final List<String> targetTables = List.of(targetTable1, targetTable2);
      final List<String> targetColumns = List.of(targetColumn1, targetColumn2);

      final Iterator<String> fkNameIt = fkNames.iterator();
      final Iterator<String> sourceTableIt = sourceTables.iterator();
      final Iterator<String> sourceColumnIt = sourceColumns.iterator();
      final Iterator<String> targetTableIt = targetTables.iterator();
      final Iterator<String> targetColumnIt = targetColumns.iterator();

      when(resultSet.getString("constraint_name")).thenAnswer(invocation -> fkNameIt.next());
      when(resultSet.getString("source_table_name")).thenAnswer(invocation -> sourceTableIt.next());
      when(resultSet.getString("source_column")).thenAnswer(invocation -> sourceColumnIt.next());
      when(resultSet.getString("referenced_table")).thenAnswer(invocation -> targetTableIt.next());
      when(resultSet.getString("referenced_column"))
          .thenAnswer(invocation -> targetColumnIt.next());

      final List<ForeignKey> result = postgresqlResultSetMapper.mapToForeignKeys(resultSet);

      assertEquals(2, result.size());

      final ForeignKey first = result.getFirst();
      assertEquals(fkName1, first.name());
      assertEquals(sourceTable1, first.sourceTable());
      assertEquals(sourceColumn1, first.sourceColumn());
      assertEquals(targetTable1, first.targetTable());
      assertEquals(targetColumn1, first.targetColumn());

      final ForeignKey second = result.get(1);
      assertEquals(fkName2, second.name());
      assertEquals(sourceTable2, second.sourceTable());
      assertEquals(sourceColumn2, second.sourceColumn());
      assertEquals(targetTable2, second.targetTable());
      assertEquals(targetColumn2, second.targetColumn());
    }

    @Test
    void ifResultSetIsEmptyItReturnsEmptyList() throws SQLException {
      when(resultSet.next()).thenReturn(false);

      final List<ForeignKey> result = postgresqlResultSetMapper.mapToForeignKeys(resultSet);

      assertTrue(result.isEmpty());
    }
  }

  @Nested
  class MapToDbEnumInfoTest {

    @Test
    void returnsEmptyListWhenResultSetHasNoRows() throws Exception {
      when(resultSet.next()).thenReturn(false);

      List<DbEnum> result = postgresqlResultSetMapper.mapToDbEnumInfo(resultSet);

      assertEquals(0, result.size());
    }

    @Test
    void mapsSingleRow() throws Exception {
      when(resultSet.next()).thenReturn(true, false);
      when(resultSet.getString("column_name")).thenReturn("col1");
      when(resultSet.getString("udt_name")).thenReturn("enum1");

      List<DbEnum> result = postgresqlResultSetMapper.mapToDbEnumInfo(resultSet);

      assertEquals(1, result.size());
      assertEquals("col1", result.getFirst().columnName());
      assertEquals("enum1", result.getFirst().enumName());
    }

    @Test
    void mapsMultipleRows() throws Exception {
      when(resultSet.next()).thenReturn(true, true, false);
      when(resultSet.getString("column_name")).thenReturn("col1", "col2");
      when(resultSet.getString("udt_name")).thenReturn("enum1", "enum2");

      List<DbEnum> result = postgresqlResultSetMapper.mapToDbEnumInfo(resultSet);

      assertEquals(2, result.size());
      assertEquals("col1", result.getFirst().columnName());
      assertEquals("enum1", result.getFirst().enumName());
      assertEquals("col2", result.get(1).columnName());
      assertEquals("enum2", result.get(1).enumName());
    }

    @Test
    void handlesNullValues() throws Exception {
      when(resultSet.next()).thenReturn(true, false);
      when(resultSet.getString("column_name")).thenReturn(null);
      when(resultSet.getString("udt_name")).thenReturn(null);

      List<DbEnum> result = postgresqlResultSetMapper.mapToDbEnumInfo(resultSet);

      assertEquals(1, result.size());
      assertNull(result.getFirst().columnName());
      assertNull(result.getFirst().enumName());
    }

    @Test
    void itDoesNotSetDbEnumValues() throws SQLException {
      when(resultSet.next()).thenReturn(true, false);
      when(resultSet.getString("column_name")).thenReturn("col1");
      when(resultSet.getString("udt_name")).thenReturn("enum1");

      List<DbEnum> result = postgresqlResultSetMapper.mapToDbEnumInfo(resultSet);

      assertEquals(0, result.getFirst().enumValues().size());
    }
  }

  @Nested
  class MapToDbEnumValuesTest {

    @Test
    void emptyResultSetReturnsEmptyList() throws SQLException {
      when(resultSet.next()).thenReturn(false);

      List<String> result = postgresqlResultSetMapper.mapToDbEnumValues(resultSet);

      assertEquals(0, result.size());
    }

    @Test
    void mapsSingleValue() throws SQLException {
      when(resultSet.next()).thenReturn(true, false);
      when(resultSet.getString("enumlabel")).thenReturn("A");

      List<String> result = postgresqlResultSetMapper.mapToDbEnumValues(resultSet);

      assertEquals("A", result.getFirst());
      assertEquals(1, result.size());
    }

    @Test
    void mapsMultipleValues() throws SQLException {
      when(resultSet.next()).thenReturn(true, true, false);
      when(resultSet.getString("enumlabel")).thenReturn("A", "B");

      List<String> result = postgresqlResultSetMapper.mapToDbEnumValues(resultSet);

      assertEquals("A", result.getFirst());
      assertEquals("B", result.get(1));
      assertEquals(2, result.size());
    }

    @Test
    void allowsNullValues() throws SQLException {
      when(resultSet.next()).thenReturn(true, false);
      when(resultSet.getString("enumlabel")).thenReturn(null);

      List<String> result = postgresqlResultSetMapper.mapToDbEnumValues(resultSet);

      assertNull(result.getFirst());
      assertEquals(1, result.size());
    }
  }
}
