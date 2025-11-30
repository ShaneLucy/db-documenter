package db.documenter.internal.formatter.impl.multiplicity;

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
    foreignKey =
        ForeignKey.builder()
            .sourceTable("source")
            .targetTable("target")
            .referencedSchema("public")
            .build();
  }

  @Nested
  class FormatTests {

    @Test
    void ifNoFormattersReturnsCurrent() {
      final var composite = new CompositeMultiplicityFormatter(List.of());
      final var result = composite.format(foreignKey, "public", "value");
      assertEquals("value", result);
    }

    @Test
    void appliesSingleFormatter() {
      when(multiplicityFormatter1.format(foreignKey, "public", "value")).thenReturn("formatted");

      final var composite = new CompositeMultiplicityFormatter(List.of(multiplicityFormatter1));
      final var result = composite.format(foreignKey, "public", "value");

      assertEquals("formatted", result);
      verify(multiplicityFormatter1, times(1)).format(foreignKey, "public", "value");
    }

    @Test
    void appliesMultipleFormattersInOrder() {
      when(multiplicityFormatter1.format(foreignKey, "public", "value")).thenReturn("v1");
      when(multiplicityFormatter2.format(foreignKey, "public", "v1")).thenReturn("v2");
      when(multiplicityFormatter3.format(foreignKey, "public", "v2")).thenReturn("v3");

      final var composite =
          new CompositeMultiplicityFormatter(
              List.of(multiplicityFormatter1, multiplicityFormatter2, multiplicityFormatter3));
      final var result = composite.format(foreignKey, "public", "value");

      assertEquals("v3", result);
      verify(multiplicityFormatter1).format(foreignKey, "public", "value");
      verify(multiplicityFormatter2).format(foreignKey, "public", "v1");
      verify(multiplicityFormatter3).format(foreignKey, "public", "v2");
    }

    @Test
    void builderAddsFormattersCorrectly() {
      when(multiplicityFormatter1.format(foreignKey, "public", "input")).thenReturn("step1");
      when(multiplicityFormatter2.format(foreignKey, "public", "step1")).thenReturn("step2");

      final var composite =
          CompositeMultiplicityFormatter.builder()
              .addFormatter(multiplicityFormatter1)
              .addFormatter(multiplicityFormatter2)
              .build();

      final var result = composite.format(foreignKey, "public", "input");

      assertEquals("step2", result);
      verify(multiplicityFormatter1).format(foreignKey, "public", "input");
      verify(multiplicityFormatter2).format(foreignKey, "public", "step1");
    }

    @Test
    void handlesNullFormatterListAsEmpty() {
      final var composite = new CompositeMultiplicityFormatter(null);
      final var result = composite.format(foreignKey, "public", "val");
      assertEquals("val", result);
    }

    @Test
    void currentCanBeNull() {
      when(multiplicityFormatter1.format(foreignKey, "public", null)).thenReturn("filled");

      final var composite = new CompositeMultiplicityFormatter(List.of(multiplicityFormatter1));
      final var result = composite.format(foreignKey, "public", null);

      assertEquals("filled", result);
      verify(multiplicityFormatter1).format(foreignKey, "public", null);
    }

    @Test
    void foreignKeyCanBeNull() {
      when(multiplicityFormatter1.format(null, "public", "val")).thenReturn("out");

      final var composite = new CompositeMultiplicityFormatter(List.of(multiplicityFormatter1));
      final var result = composite.format(null, "public", "val");

      assertEquals("out", result);
      verify(multiplicityFormatter1).format(null, "public", "val");
    }
  }
}
