package db.documenter.internal.formatter.impl.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import db.documenter.internal.formatter.api.EntityLineFormatter;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Table;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompositeEntityLineFormatterTest {

  private Table table;
  private Column column;
  @Mock private EntityLineFormatter lineFormatter1;
  @Mock private EntityLineFormatter lineFormatter2;
  @Mock private EntityLineFormatter lineFormatter3;

  @BeforeEach
  void setUp() {
    Mockito.reset(lineFormatter1, lineFormatter2, lineFormatter3);
    table = Table.builder().build();
    column = Column.builder().name("col").build();
  }

  @Nested
  class FormatTests {

    @Test
    void ifNoFormattersReturnsCurrent() {
      final var composite = new CompositeEntityLineFormatter(List.of());
      final var result = composite.format(table, column, "value");
      assertEquals("value", result);
    }

    @Test
    void appliesSingleFormatter() {
      when(lineFormatter1.format(table, column, "value")).thenReturn("formatted");

      final var composite = new CompositeEntityLineFormatter(List.of(lineFormatter1));
      final var result = composite.format(table, column, "value");

      assertEquals("formatted", result);
      verify(lineFormatter1, times(1)).format(table, column, "value");
    }

    @Test
    void appliesMultipleFormattersInOrder() {
      when(lineFormatter1.format(table, column, "value")).thenReturn("v1");
      when(lineFormatter2.format(table, column, "v1")).thenReturn("v2");
      when(lineFormatter3.format(table, column, "v2")).thenReturn("v3");

      final var composite =
          new CompositeEntityLineFormatter(List.of(lineFormatter1, lineFormatter2, lineFormatter3));
      final var result = composite.format(table, column, "value");

      assertEquals("v3", result);
      verify(lineFormatter1).format(table, column, "value");
      verify(lineFormatter2).format(table, column, "v1");
      verify(lineFormatter3).format(table, column, "v2");
    }

    @Test
    void builderAddsFormattersCorrectly() {
      when(lineFormatter1.format(table, column, "input")).thenReturn("step1");
      when(lineFormatter2.format(table, column, "step1")).thenReturn("step2");

      final var composite =
          CompositeEntityLineFormatter.builder()
              .addFormatter(lineFormatter1)
              .addFormatter(lineFormatter2)
              .build();

      final var result = composite.format(table, column, "input");

      assertEquals("step2", result);
      verify(lineFormatter1).format(table, column, "input");
      verify(lineFormatter2).format(table, column, "step1");
    }

    @Test
    void handlesNullFormatterListAsEmpty() {
      final var composite = new CompositeEntityLineFormatter(null);
      final var result = composite.format(table, column, "val");
      assertEquals("val", result);
    }

    @Test
    void currentCanBeNull() {
      when(lineFormatter1.format(table, column, null)).thenReturn("filled");

      final var composite = new CompositeEntityLineFormatter(List.of(lineFormatter1));
      final var result = composite.format(table, column, null);

      assertEquals("filled", result);
      verify(lineFormatter1).format(table, column, null);
    }

    @Test
    void tableCanBeNull() {
      when(lineFormatter1.format(null, column, "val")).thenReturn("out");

      final var composite = new CompositeEntityLineFormatter(List.of(lineFormatter1));
      final var result = composite.format(null, column, "val");

      assertEquals("out", result);
      verify(lineFormatter1).format(null, column, "val");
    }

    @Test
    void columnCanBeNull() {
      when(lineFormatter1.format(table, null, "val")).thenReturn("out");

      final var composite = new CompositeEntityLineFormatter(List.of(lineFormatter1));
      final var result = composite.format(table, null, "val");

      assertEquals("out", result);
      verify(lineFormatter1).format(table, null, "val");
    }
  }
}
