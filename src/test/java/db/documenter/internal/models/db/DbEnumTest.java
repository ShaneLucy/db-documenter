package db.documenter.internal.models.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DbEnumTest {

  @Nested
  class CombineDbEnumAndValuesTests {
    @Test
    void itWorksCorrectly() {
      final List<String> enumValues = List.of("value1", "value 2", "value3");
      final var providedDbEnum =
          DbEnum.builder().columnName("column name").enumName("enum name").build();

      final var result = DbEnum.combineDbEnumValuesAndInfo(providedDbEnum, enumValues);

      assertEquals(enumValues, result.enumValues());
      assertEquals(providedDbEnum.columnName(), result.columnName());
      assertEquals(providedDbEnum.enumName(), result.enumName());
    }
  }
}
