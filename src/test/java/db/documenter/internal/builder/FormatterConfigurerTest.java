package db.documenter.internal.builder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import db.documenter.internal.formatter.api.EntityLineFormatter;
import db.documenter.internal.formatter.api.MultiplicityFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FormatterConfigurerTest {

  private FormatterConfigurer formatterConfigurer;

  @BeforeEach
  void setUp() {
    formatterConfigurer = new FormatterConfigurer();
  }

  @Nested
  class CreateEntityLineFormatterTests {

    @Test
    void returnsNonNullEntityLineFormatter() {
      final EntityLineFormatter formatter = formatterConfigurer.createEntityLineFormatter();

      assertNotNull(formatter);
    }

    @Test
    void returnsNewInstanceOnEachCall() {
      final EntityLineFormatter formatter1 = formatterConfigurer.createEntityLineFormatter();
      final EntityLineFormatter formatter2 = formatterConfigurer.createEntityLineFormatter();

      assertNotSame(formatter1, formatter2);
    }
  }

  @Nested
  class CreateMultiplicityFormatterTests {

    @Test
    void returnsNonNullMultiplicityFormatter() {
      final MultiplicityFormatter formatter = formatterConfigurer.createMultiplicityFormatter();

      assertNotNull(formatter);
    }

    @Test
    void returnsNewInstanceOnEachCall() {
      final MultiplicityFormatter formatter1 = formatterConfigurer.createMultiplicityFormatter();
      final MultiplicityFormatter formatter2 = formatterConfigurer.createMultiplicityFormatter();

      assertNotSame(formatter1, formatter2);
    }
  }
}
