package dev.shanelucy.dbdocumenter.internal.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.shanelucy.dbdocumenter.internal.exceptions.ValidationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ReadRequestTest {

  @Nested
  class ValidationTests {

    @Test
    void itThrowsExceptionWhenSchemasIsNull() {
      final var exception = assertThrows(ValidationException.class, () -> new ReadRequest(null));

      assertEquals("schemas must contain at least 1 item", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenSchemasIsEmpty() {
      final var exception =
          assertThrows(ValidationException.class, () -> new ReadRequest(List.of()));

      assertEquals("schemas must contain at least 1 item", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenSchemasContainsNull() {
      final var schemas = Arrays.asList("app", null);

      final var exception = assertThrows(ValidationException.class, () -> new ReadRequest(schemas));

      assertEquals("schemas must not contain null elements", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenSchemasContainsEmptyName() {
      final var exception =
          assertThrows(ValidationException.class, () -> new ReadRequest(List.of("app", "")));

      assertEquals("schemas element must not be empty", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenSchemasContainsDuplicate() {
      final var exception =
          assertThrows(
              ValidationException.class, () -> new ReadRequest(List.of("app", "auth", "app")));

      assertEquals(
          "schemas must not contain duplicates, found duplicate: app", exception.getMessage());
    }

    @Test
    void itAcceptsWhitespaceOnlySchemaName() {
      final var request = new ReadRequest(List.of(" "));

      assertEquals(List.of(" "), request.schemas());
    }

    @Test
    void itTreatsNamesDifferingOnlyInCaseAsDistinct() {
      final var request = new ReadRequest(List.of("app", "App"));

      assertEquals(List.of("app", "App"), request.schemas());
    }
  }

  @Nested
  class SchemasTests {

    @Test
    void itPreservesSchemaOrder() {
      final var request = new ReadRequest(List.of("zeta", "alpha", "mid"));

      assertEquals(List.of("zeta", "alpha", "mid"), request.schemas());
    }

    @Test
    void itDefensivelyCopiesSchemas() {
      final var schemas = new ArrayList<>(List.of("app"));
      final var request = new ReadRequest(schemas);

      schemas.add("auth");

      assertEquals(List.of("app"), request.schemas());
    }

    @Test
    void itReturnsUnmodifiableSchemas() {
      final var request = new ReadRequest(List.of("app"));
      final var schemas = request.schemas();

      assertThrows(UnsupportedOperationException.class, () -> schemas.add("auth"));
    }
  }

  @Nested
  class BuilderTests {

    @Test
    void itBuildsRequestWithSchemas() {
      final var request = ReadRequest.builder().schemas(List.of("app", "auth")).build();

      assertEquals(new ReadRequest(List.of("app", "auth")), request);
    }

    @Test
    void itThrowsExceptionWhenSchemasIsUnset() {
      final var builder = ReadRequest.builder();

      final var exception = assertThrows(ValidationException.class, builder::build);

      assertEquals("schemas must not be null", exception.getMessage());
    }
  }
}
