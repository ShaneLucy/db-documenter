package db.documenter;

import static org.junit.jupiter.api.Assertions.*;

import db.documenter.internal.exceptions.ValidationException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
      final int expectedPort = 2346782;
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
}
