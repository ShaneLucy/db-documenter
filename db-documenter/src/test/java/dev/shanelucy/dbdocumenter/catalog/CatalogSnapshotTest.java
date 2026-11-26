package dev.shanelucy.dbdocumenter.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CatalogSnapshotTest {

  private static Schema schema(final String name) {
    return Schema.builder().name(name).build();
  }

  private static CatalogSnapshot.Builder postgres() {
    return CatalogSnapshot.builder().productName("PostgreSQL").productVersion("18.1");
  }

  @Nested
  class BuilderTests {

    @Test
    void itDefaultsSchemasToEmpty() {
      final var snapshot = postgres().build();

      assertEquals("PostgreSQL", snapshot.productName());
      assertEquals("18.1", snapshot.productVersion());
      assertTrue(snapshot.schemas().isEmpty());
    }

    @Test
    void itBuildsSnapshotWithSchemas() {
      final var snapshot = postgres().schemas(List.of(schema("app"))).build();

      assertEquals(new CatalogSnapshot("PostgreSQL", "18.1", List.of(schema("app"))), snapshot);
    }

    @Test
    void itThrowsExceptionWhenProductNameIsNotSet() {
      final var builder = CatalogSnapshot.builder().productVersion("18.1");

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("productName must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenProductVersionIsNotSet() {
      final var builder = CatalogSnapshot.builder().productName("PostgreSQL");

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("productVersion must not be null", exception.getMessage());
    }
  }

  @Nested
  class ValidationTests {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void itThrowsExceptionWhenProductNameIsBlank(final String productName) {
      final List<Schema> schemas = List.of();

      final var exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new CatalogSnapshot(productName, "18.1", schemas));

      assertEquals("productName must not be blank", exception.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void itThrowsExceptionWhenProductVersionIsBlank(final String productVersion) {
      final List<Schema> schemas = List.of();

      final var exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new CatalogSnapshot("PostgreSQL", productVersion, schemas));

      assertEquals("productVersion must not be blank", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenSchemasAreNull() {
      final var builder = postgres().schemas(null);

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("schemas must not be null", exception.getMessage());
    }

    @Test
    void itThrowsExceptionWhenSchemasContainNull() {
      final var builder = postgres().schemas(Arrays.asList(schema("app"), null));

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals("schemas must not contain null elements", exception.getMessage());
    }

    @Test
    void itThrowsExceptionNamingDuplicateSchema() {
      final var builder = postgres().schemas(List.of(schema("app"), schema("auth"), schema("app")));

      final var exception = assertThrows(IllegalArgumentException.class, builder::build);

      assertEquals(
          "schemas.name must not contain duplicates, found duplicate: app", exception.getMessage());
    }

    @Test
    void itTreatsSchemaNamesAsCaseSensitive() {
      final var snapshot = postgres().schemas(List.of(schema("app"), schema("APP"))).build();

      assertEquals(2, snapshot.schemas().size());
    }
  }

  @Nested
  class OrderingTests {

    @Test
    void itSortsSchemasByNameInStringOrder() {
      final var snapshot =
          postgres()
              .schemas(List.of(schema("public"), schema("auth"), schema("Audit"), schema("app")))
              .build();

      assertEquals(
          List.of("Audit", "app", "auth", "public"),
          snapshot.schemas().stream().map(Schema::name).toList());
    }

    @Test
    void itProducesEqualSnapshotsWhateverTheInputOrder() {
      final var forwards = postgres().schemas(List.of(schema("app"), schema("auth"))).build();
      final var backwards = postgres().schemas(List.of(schema("auth"), schema("app"))).build();

      assertEquals(forwards, backwards);
    }
  }

  @Nested
  class DefensiveCopyTests {

    @Test
    void itDefensivelyCopiesSchemas() {
      final var schemas = new ArrayList<>(List.of(schema("app")));
      final var snapshot = postgres().schemas(schemas).build();

      schemas.add(schema("auth"));

      assertEquals(List.of(schema("app")), snapshot.schemas());
    }

    @Test
    void itReturnsUnmodifiableSchemas() {
      final var snapshot = postgres().schemas(new ArrayList<>(List.of(schema("app")))).build();
      final var schemas = snapshot.schemas();

      assertThrows(UnsupportedOperationException.class, () -> schemas.add(schema("auth")));
    }
  }
}
