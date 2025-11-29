package db.documenter.internal.formatter.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import db.documenter.internal.formatter.api.MultiplicityFormatter;
import db.documenter.internal.models.db.ForeignKey;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompositeMultiplicityFormatterTest {

  private ForeignKey foreignKey;
  @Mock private MultiplicityFormatter multiplicityFormatter1;
  @Mock private MultiplicityFormatter multiplicityFormatter2;
  @Mock private MultiplicityFormatter multiplicityFormatter3;

  @BeforeEach
  void setUp() {
    Mockito.reset(multiplicityFormatter1, multiplicityFormatter2, multiplicityFormatter3);
    foreignKey = ForeignKey.builder().sourceTable("source").targetTable("target").build();
  }

  @Nested
  class FormatTests {

    @Test
    void ifNoFormattersReturnsCurrent() {
      final var composite = new CompositeMultiplicityFormatter(List.of());
      final var result = composite.format(foreignKey, "value");
      assertEquals("value", result);
    }

    @Test
    void appliesSingleFormatter() {
      when(multiplicityFormatter1.format(foreignKey, "value")).thenReturn("formatted");

      final var composite = new CompositeMultiplicityFormatter(List.of(multiplicityFormatter1));
      final var result = composite.format(foreignKey, "value");

      assertEquals("formatted", result);
      verify(multiplicityFormatter1, times(1)).format(foreignKey, "value");
    }

    @Test
    void appliesMultipleFormattersInOrder() {
      when(multiplicityFormatter1.format(foreignKey, "value")).thenReturn("v1");
      when(multiplicityFormatter2.format(foreignKey, "v1")).thenReturn("v2");
      when(multiplicityFormatter3.format(foreignKey, "v2")).thenReturn("v3");

      final var composite =
          new CompositeMultiplicityFormatter(
              List.of(multiplicityFormatter1, multiplicityFormatter2, multiplicityFormatter3));
      final var result = composite.format(foreignKey, "value");

      assertEquals("v3", result);
      verify(multiplicityFormatter1).format(foreignKey, "value");
      verify(multiplicityFormatter2).format(foreignKey, "v1");
      verify(multiplicityFormatter3).format(foreignKey, "v2");
    }

    @Test
    void builderAddsFormattersCorrectly() {
      when(multiplicityFormatter1.format(foreignKey, "input")).thenReturn("step1");
      when(multiplicityFormatter2.format(foreignKey, "step1")).thenReturn("step2");

      final var composite =
          CompositeMultiplicityFormatter.builder()
              .addFormatter(multiplicityFormatter1)
              .addFormatter(multiplicityFormatter2)
              .build();

      final var result = composite.format(foreignKey, "input");

      assertEquals("step2", result);
      verify(multiplicityFormatter1).format(foreignKey, "input");
      verify(multiplicityFormatter2).format(foreignKey, "step1");
    }

    @Test
    void handlesNullFormatterListAsEmpty() {
      final var composite = new CompositeMultiplicityFormatter(null);
      final var result = composite.format(foreignKey, "val");
      assertEquals("val", result);
    }

    @Test
    void currentCanBeNull() {
      when(multiplicityFormatter1.format(foreignKey, null)).thenReturn("filled");

      final var composite = new CompositeMultiplicityFormatter(List.of(multiplicityFormatter1));
      final var result = composite.format(foreignKey, null);

      assertEquals("filled", result);
      verify(multiplicityFormatter1).format(foreignKey, null);
    }

    @Test
    void foreignKeyCanBeNull() {
      when(multiplicityFormatter1.format(null, "val")).thenReturn("out");

      final var composite = new CompositeMultiplicityFormatter(List.of(multiplicityFormatter1));
      final var result = composite.format(null, "val");

      assertEquals("out", result);
      verify(multiplicityFormatter1).format(null, "val");
    }
  }
}
