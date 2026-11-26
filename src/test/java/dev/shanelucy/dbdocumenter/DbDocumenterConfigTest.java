package db.documenter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import db.documenter.internal.exceptions.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class DbDocumenterConfigTest {

  @Nested
  class ConstructorTests {

    @Test
    void whenAllFieldsAreValidItConstructsTheObject() {
      final var schemas = List.of("schema");
      assertDoesNotThrow(
          () ->
              new DbDocumenterConfig(
                  schemas,
                  "host",
                  1234,
                  "database name",
                  false,
                  "username",
                  "password",
                  DatabaseType.POSTGRESQL));
    }

    @Test
    void schemaMustContainAtLeast1Item() {
      final var emptyList = new ArrayList<String>();
      assertThrows(
          ValidationException.class,
          () ->
              new DbDocumenterConfig(
                  emptyList,
                  "host",
                  1234,
                  "database name",
                  false,
                  "username",
                  "password",
                  DatabaseType.POSTGRESQL));
    }

    @Test
    void databaseHostMustNotBeBlank() {
      final var schemas = List.of("schema");
      assertThrows(
          ValidationException.class,
          () ->
              new DbDocumenterConfig(
                  schemas,
                  "",
                  1234,
                  "database name",
                  false,
                  "username",
                  "password",
                  DatabaseType.POSTGRESQL));
    }

    @Test
    void databaseNameMustNotBeBlank() {
      final var schemas = List.of("schema");
      assertThrows(
          ValidationException.class,
          () ->
              new DbDocumenterConfig(
                  schemas,
                  "host",
                  1234,
                  "",
                  false,
                  "username",
                  "password",
                  DatabaseType.POSTGRESQL));
    }

    @Test
    void usernameMustNotBeBlank() {
      final var schemas = List.of("schema");
      assertThrows(
          ValidationException.class,
          () ->
              new DbDocumenterConfig(
                  schemas,
                  "host",
                  1234,
                  "database name",
                  false,
                  "",
                  "password",
                  DatabaseType.POSTGRESQL));
    }

    @Test
    void passwordMustNotBeBlank() {
      final var schemas = List.of("schema");
      assertThrows(
          ValidationException.class,
          () ->
              new DbDocumenterConfig(
                  schemas,
                  "host",
                  1234,
                  "database name",
                  false,
                  "username",
                  "",
                  DatabaseType.POSTGRESQL));
    }
  }

  @Nested
  class PortValidationTests {

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 65536, 2346782})
    void itThrowsExceptionWhenPortIsOutOfRange(final int port) {
      final var schemas = List.of("schema");

      final var exception =
          assertThrows(
              ValidationException.class,
              () ->
                  new DbDocumenterConfig(
                      schemas,
                      "host",
                      port,
                      "database name",
                      false,
                      "username",
                      "password",
                      DatabaseType.POSTGRESQL));

      assertEquals("databasePort must be between 1 and 65535", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5432, 65535})
    void itAcceptsPortsInRange(final int port) {
      final var schemas = List.of("schema");

      assertDoesNotThrow(
          () ->
              new DbDocumenterConfig(
                  schemas,
                  "host",
                  port,
                  "database name",
                  false,
                  "username",
                  "password",
                  DatabaseType.POSTGRESQL));
    }
  }

  @Nested
  class PublicContractTests {

    @Test
    void itMasksThePasswordInToString() {
      final var config =
          new DbDocumenterConfig(
              List.of("public"),
              "host",
              5432,
              "db",
              true,
              "user",
              "s3cret",
              DatabaseType.POSTGRESQL);

      assertEquals(
          "DbDocumenterConfig[schemas=[public], databaseHost=host, databasePort=5432,"
              + " databaseName=db, useSsl=true, username=user, password=****,"
              + " databaseType=POSTGRESQL]",
          config.toString());
    }

    @Test
    void itThrowsExceptionCatchableAsIllegalArgumentExceptionWhenSchemaIsNull() {
      final var schemas = new ArrayList<String>();
      schemas.add(null);

      final var exception =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new DbDocumenterConfig(
                      schemas,
                      "host",
                      5432,
                      "db",
                      true,
                      "user",
                      "password",
                      DatabaseType.POSTGRESQL));

      assertEquals("schemas must not contain null elements", exception.getMessage());
    }

    @Test
    void itThrowsValidationExceptionWhenBuilderSchemasIsNull() {
      final var builder =
          DbDocumenterConfig.builder()
              .databaseHost("host")
              .databaseName("db")
              .username("user")
              .password("password");

      final var exception = assertThrows(ValidationException.class, builder::build);

      assertEquals("schemas must contain at least 1 item", exception.getMessage());
    }
  }

  @Nested
  class BuilderTests {

    @Test
    void whenAllFieldsAreValidItConstructsTheObject() {
      assertDoesNotThrow(
          () ->
              DbDocumenterConfig.builder()
                  .schemas(List.of("schema"))
                  .databaseHost("database host")
                  .databaseName("database name")
                  .databasePort(5432)
                  .useSsl(true)
                  .username("username")
                  .password("password")
                  .databaseType(DatabaseType.POSTGRESQL)
                  .build());
    }

    @Test
    void ifDataBasePortProvidedInBuilderItOverridesDefault() {
      final int expectedPort = 6543;
      final var result =
          DbDocumenterConfig.builder()
              .schemas(List.of("schema"))
              .databaseHost("database host")
              .databaseName("database name")
              .databasePort(expectedPort)
              .useSsl(true)
              .username("username")
              .password("password")
              .databaseType(DatabaseType.POSTGRESQL)
              .build();

      assertEquals(expectedPort, result.databasePort());
    }

    @Test
    void ifUseSslProvidedInBuilderItOverridesDefault() {
      final var result =
          DbDocumenterConfig.builder()
              .schemas(List.of("schema"))
              .databaseHost("database host")
              .databaseName("database name")
              .databasePort(5432)
              .useSsl(false)
              .username("username")
              .password("password")
              .databaseType(DatabaseType.POSTGRESQL)
              .build();

      assertFalse(result.useSsl());
    }

    @Test
    void schemaMustContainAtLeast1Item() {
      final var emptyList = new ArrayList<String>();
      assertThrows(
          ValidationException.class,
          () ->
              DbDocumenterConfig.builder()
                  .schemas(emptyList)
                  .databaseHost("database host")
                  .databasePort(5432)
                  .databaseName("database name")
                  .useSsl(true)
                  .username("username")
                  .password("password")
                  .databaseType(DatabaseType.POSTGRESQL)
                  .build());
    }

    @Test
    void databaseHostMustNotBeBlank() {
      assertThrows(
          ValidationException.class,
          () ->
              DbDocumenterConfig.builder()
                  .schemas(List.of("schema"))
                  .databaseHost("")
                  .databasePort(5432)
                  .databaseName("database name")
                  .useSsl(true)
                  .username("username")
                  .password("password")
                  .databaseType(DatabaseType.POSTGRESQL)
                  .build());
    }

    @Test
    void databaseNameMustNotBeBlank() {
      assertThrows(
          ValidationException.class,
          () ->
              DbDocumenterConfig.builder()
                  .schemas(List.of("schema"))
                  .databaseHost("database host")
                  .databaseName("")
                  .databasePort(5432)
                  .useSsl(true)
                  .username("username")
                  .password("password")
                  .databaseType(DatabaseType.POSTGRESQL)
                  .build());
    }

    @Test
    void usernameMustNotBeBlank() {
      assertThrows(
          ValidationException.class,
          () ->
              DbDocumenterConfig.builder()
                  .schemas(List.of("schema"))
                  .databaseHost("database host")
                  .databaseName("database name")
                  .databasePort(5432)
                  .useSsl(true)
                  .username("")
                  .password("password")
                  .databaseType(DatabaseType.POSTGRESQL)
                  .build());
    }

    @Test
    void passwordMustNotBeBlank() {
      final var schemas = List.of("schema");
      assertThrows(
          ValidationException.class,
          () ->
              DbDocumenterConfig.builder()
                  .schemas(schemas)
                  .databaseHost("database host")
                  .databaseName("database name")
                  .databasePort(5432)
                  .useSsl(true)
                  .username("username")
                  .password("")
                  .databaseType(DatabaseType.POSTGRESQL)
                  .build());
    }

    @Test
    void portDefaultsTo5432() {
      final var result =
          DbDocumenterConfig.builder()
              .schemas(List.of("schema"))
              .databaseHost("database host")
              .databaseName("database name")
              .useSsl(true)
              .username("username")
              .password("password")
              .databaseType(DatabaseType.POSTGRESQL)
              .build();

      assertEquals(5432, result.databasePort());
    }

    @Test
    void databaseTypeDefaultsToPostgres() {
      final var result =
          DbDocumenterConfig.builder()
              .schemas(List.of("schema"))
              .databaseHost("database host")
              .databaseName("database name")
              .databasePort(5432)
              .useSsl(true)
              .username("username")
              .password("password")
              .build();

      assertEquals(DatabaseType.POSTGRESQL, result.databaseType());
    }

    @Test
    void useSslDefaultsToTrue() {
      final var result =
          DbDocumenterConfig.builder()
              .schemas(List.of("schema"))
              .databaseHost("database host")
              .databaseName("database name")
              .databasePort(5432)
              .username("username")
              .password("password")
              .databaseType(DatabaseType.POSTGRESQL)
              .build();

      assertTrue(result.useSsl());
    }
  }

  @Nested
  class BuilderRequiredFieldTests {

    @ParameterizedTest
    @MethodSource("incompleteBuilders")
    void itThrowsValidationExceptionWhenRequiredFieldIsMissing(
        final String expectedMessage,
        final Supplier<DbDocumenterConfig.Builder> incompleteBuilder) {
      final var builder = incompleteBuilder.get();

      final var exception = assertThrows(ValidationException.class, builder::build);

      assertEquals(expectedMessage, exception.getMessage());
    }

    static Stream<Arguments> incompleteBuilders() {
      return Stream.of(
          Arguments.of(
              "schemas must contain at least 1 item",
              (Supplier<DbDocumenterConfig.Builder>)
                  () ->
                      DbDocumenterConfig.builder()
                          .databaseHost("host")
                          .databaseName("db")
                          .username("user")
                          .password("pass")),
          Arguments.of(
              "databaseHost must not be blank",
              (Supplier<DbDocumenterConfig.Builder>)
                  () ->
                      DbDocumenterConfig.builder()
                          .schemas(List.of("schema"))
                          .databaseName("db")
                          .username("user")
                          .password("pass")),
          Arguments.of(
              "databaseName must not be blank",
              (Supplier<DbDocumenterConfig.Builder>)
                  () ->
                      DbDocumenterConfig.builder()
                          .schemas(List.of("schema"))
                          .databaseHost("host")
                          .username("user")
                          .password("pass")),
          Arguments.of(
              "username must not be blank",
              (Supplier<DbDocumenterConfig.Builder>)
                  () ->
                      DbDocumenterConfig.builder()
                          .schemas(List.of("schema"))
                          .databaseHost("host")
                          .databaseName("db")
                          .password("pass")),
          Arguments.of(
              "password must not be blank",
              (Supplier<DbDocumenterConfig.Builder>)
                  () ->
                      DbDocumenterConfig.builder()
                          .schemas(List.of("schema"))
                          .databaseHost("host")
                          .databaseName("db")
                          .username("user")));
    }
  }
}
