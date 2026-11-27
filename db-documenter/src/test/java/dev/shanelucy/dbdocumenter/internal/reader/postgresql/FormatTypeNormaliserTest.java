package dev.shanelucy.dbdocumenter.internal.reader.postgresql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class FormatTypeNormaliserTest {

  @Nested
  class NormaliseTests {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
        delimiter = '|',
        value = {
          // search_path shadowing and quote_all_identifiers, alone and together
          "\"pg_catalog\".\"text\"[]|text[]",
          "\"pg_catalog\".\"text\"|text",
          "pg_catalog.text|text",
          "pg_catalog.text[]|text[]",
          "\"text\"|text",
          "\"text\"[]|text[]",
          "\"json\"|json",
          "\"_int4\"|_int4",
          "\"int4\"|int4",
          // a type modifier after the quoted name is kept
          "\"bit\"(3)|\"bit\"(3)",
          "\"text\"(5)|text(5)",
          "pg_catalog.\"char\"[]|\"char\"[]",
          "\"pg_catalog\".\"char\"|\"char\"",
          // names default quoting keeps quoted
          "\"a\"\"b\"|\"a\"\"b\"",
          "\"a\"\"b\"[]|\"a\"\"b\"[]",
          "\"pg_catalog\".\"a\"\"b\"|\"a\"\"b\"",
          "\"Mood\"|\"Mood\"",
          "\"9abc\"|\"9abc\"",
          "\"a b\"|\"a b\"",
          "\"\"|\"\"",
          // unterminated quotes are left alone, after any prefix is removed
          "\"text|\"text",
          "\"a\"\"|\"a\"\"",
          "\"pg_catalog\".\"text|\"text",
          // prefix edge cases
          "pg_catalog.|''",
          "\"pg_catalog\".|''",
          "\"pg_catalog\"|pg_catalog",
          "pg_catalogtext|pg_catalogtext",
          "x.pg_catalog.text|x.pg_catalog.text",
          "''|''"
        })
    void itNormalisesToDefaultSessionSpelling(final String formattedName, final String expected) {
      assertEquals(expected, FormatTypeNormaliser.normalise(formattedName));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "\"any\"",
          "\"bit\"",
          "\"char\"",
          "\"interval\"",
          "\"numeric\"",
          "\"time\"",
          "\"timestamp\"",
          "\"varchar\""
        })
    void itKeepsKeywordTypeNamesQuoted(final String formattedName) {
      assertEquals(formattedName, FormatTypeNormaliser.normalise(formattedName));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "integer",
          "character varying(20)[]",
          "numeric(10,2)",
          "timestamp with time zone",
          "\"char\"",
          "text[]",
          "bit(3)"
        })
    void itLeavesDefaultOutputUnchanged(final String formattedName) {
      assertEquals(formattedName, FormatTypeNormaliser.normalise(formattedName));
    }
  }
}
