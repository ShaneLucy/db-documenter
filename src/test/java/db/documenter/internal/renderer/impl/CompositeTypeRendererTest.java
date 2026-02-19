package db.documenter.internal.renderer.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import db.documenter.internal.models.db.CompositeField;
import db.documenter.internal.models.db.DbCompositeType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CompositeTypeRendererTest {

  private CompositeTypeRenderer compositeTypeRenderer;

  @BeforeEach
  void setUp() {
    compositeTypeRenderer = new CompositeTypeRenderer();
  }

  @Nested
  class RenderTests {

    @Test
    void rendersSimpleCompositeTypeWithTwoFields() {
      final var field1 =
          CompositeField.builder().fieldName("street").fieldType("text").position(1).build();
      final var field2 =
          CompositeField.builder().fieldName("city").fieldType("text").position(2).build();

      final var compositeType =
          DbCompositeType.builder()
              .typeName("address")
              .schemaName("public")
              .fields(List.of(field1, field2))
              .build();

      final var result = compositeTypeRenderer.render(compositeType);

      final var expectedOutput =
          """
          	entity "address" <<composite>> {
          		street : text
          		city : text
          	}
          """;

      assertEquals(expectedOutput, result);
    }

    @Test
    void rendersCompositeTypeWithMultipleFields() {
      final var field1 =
          CompositeField.builder()
              .fieldName("street_line1")
              .fieldType("character varying(200)")
              .position(1)
              .build();
      final var field2 =
          CompositeField.builder()
              .fieldName("street_line2")
              .fieldType("character varying(200)")
              .position(2)
              .build();
      final var field3 =
          CompositeField.builder()
              .fieldName("city")
              .fieldType("character varying(100)")
              .position(3)
              .build();
      final var field4 =
          CompositeField.builder()
              .fieldName("postal_code")
              .fieldType("character varying(10)")
              .position(4)
              .build();

      final var compositeType =
          DbCompositeType.builder()
              .typeName("address")
              .schemaName("core")
              .fields(List.of(field1, field2, field3, field4))
              .build();

      final var result = compositeTypeRenderer.render(compositeType);

      final var expectedOutput =
          """
          	entity "address" <<composite>> {
          		street_line1 : character varying(200)
          		street_line2 : character varying(200)
          		city : character varying(100)
          		postal_code : character varying(10)
          	}
          """;

      assertEquals(expectedOutput, result);
    }

    @Test
    void rendersCompositeTypeWithVariousPostgresqlTypes() {
      final var field1 =
          CompositeField.builder().fieldName("id").fieldType("uuid").position(1).build();
      final var field2 =
          CompositeField.builder().fieldName("name").fieldType("text").position(2).build();
      final var field3 =
          CompositeField.builder().fieldName("age").fieldType("integer").position(3).build();
      final var field4 =
          CompositeField.builder()
              .fieldName("balance")
              .fieldType("numeric(10,2)")
              .position(4)
              .build();
      final var field5 =
          CompositeField.builder()
              .fieldName("created_at")
              .fieldType("timestamp without time zone")
              .position(5)
              .build();
      final var field6 =
          CompositeField.builder().fieldName("is_active").fieldType("boolean").position(6).build();

      final var compositeType =
          DbCompositeType.builder()
              .typeName("person_record")
              .schemaName("public")
              .fields(List.of(field1, field2, field3, field4, field5, field6))
              .build();

      final var result = compositeTypeRenderer.render(compositeType);

      final var expectedOutput =
          """
          	entity "person_record" <<composite>> {
          		id : uuid
          		name : text
          		age : integer
          		balance : numeric(10,2)
          		created_at : timestamp without time zone
          		is_active : boolean
          	}
          """;

      assertEquals(expectedOutput, result);
    }

    @Test
    void rendersCompositeTypeWithEmptyFields() {
      final var compositeType =
          DbCompositeType.builder()
              .typeName("empty_type")
              .schemaName("public")
              .fields(List.of())
              .build();

      final var result = compositeTypeRenderer.render(compositeType);

      final var expectedOutput =
          """
          	entity "empty_type" <<composite>> {
          	}
          """;

      assertEquals(expectedOutput, result);
    }

    @Test
    void usesCompositeStereotypeNotEnumStereotype() {
      final var field =
          CompositeField.builder().fieldName("value").fieldType("text").position(1).build();

      final var compositeType =
          DbCompositeType.builder()
              .typeName("test_type")
              .schemaName("public")
              .fields(List.of(field))
              .build();

      final var result = compositeTypeRenderer.render(compositeType);

      assertTrue(result.contains("<<composite>>"), "Stereotype should be <<composite>>");
      assertTrue(
          !result.contains("<<enum>>"),
          "Stereotype should NOT be <<enum>> - it should be <<composite>>");
    }

    @Test
    void usesCorrectFieldFormat() {
      final var field =
          CompositeField.builder()
              .fieldName("amount")
              .fieldType("numeric(10,2)")
              .position(1)
              .build();

      final var compositeType =
          DbCompositeType.builder()
              .typeName("money")
              .schemaName("public")
              .fields(List.of(field))
              .build();

      final var result = compositeTypeRenderer.render(compositeType);

      assertTrue(
          result.contains("amount : numeric(10,2)"),
          "Field format should be: field_name : field_type");
    }

    @Test
    void usesProperIndentationWithTabs() {
      final var field =
          CompositeField.builder().fieldName("test").fieldType("text").position(1).build();

      final var compositeType =
          DbCompositeType.builder()
              .typeName("test_type")
              .schemaName("public")
              .fields(List.of(field))
              .build();

      final var result = compositeTypeRenderer.render(compositeType);

      assertTrue(result.startsWith("\tentity"), "Entity should start with single tab");
      assertTrue(result.contains("\t\ttest : text"), "Fields should start with two tabs");
      assertTrue(result.contains("\t}\n"), "Closing brace should start with single tab");
    }

    @Test
    void rendersTypeNameWithoutSchemaQualification() {
      final var field =
          CompositeField.builder().fieldName("value").fieldType("text").position(1).build();

      final var compositeType =
          DbCompositeType.builder()
              .typeName("address")
              .schemaName("core")
              .fields(List.of(field))
              .build();

      final var result = compositeTypeRenderer.render(compositeType);

      assertTrue(result.contains("\"address\""), "Type name should be rendered");
      assertTrue(
          !result.contains("core.address"),
          "Schema should NOT be included in type name - types are already in schema package");
    }

    @Test
    void preservesFieldOrderFromList() {
      final var field1 =
          CompositeField.builder().fieldName("zebra").fieldType("text").position(3).build();
      final var field2 =
          CompositeField.builder().fieldName("apple").fieldType("text").position(1).build();
      final var field3 =
          CompositeField.builder().fieldName("middle").fieldType("text").position(2).build();

      final var compositeType =
          DbCompositeType.builder()
              .typeName("test_type")
              .schemaName("public")
              .fields(List.of(field1, field2, field3))
              .build();

      final var result = compositeTypeRenderer.render(compositeType);

      final int zebraIndex = result.indexOf("zebra");
      final int appleIndex = result.indexOf("apple");
      final int middleIndex = result.indexOf("middle");

      assertTrue(
          zebraIndex < appleIndex && appleIndex < middleIndex,
          "Fields should be rendered in list order, not alphabetically");
    }

    @Test
    void rendersComplexMoneyTypeExample() {
      final var field1 =
          CompositeField.builder()
              .fieldName("amount")
              .fieldType("numeric(10,2)")
              .position(1)
              .build();
      final var field2 =
          CompositeField.builder()
              .fieldName("currency_code")
              .fieldType("character(3)")
              .position(2)
              .build();

      final var compositeType =
          DbCompositeType.builder()
              .typeName("money_amount")
              .schemaName("core")
              .fields(List.of(field1, field2))
              .build();

      final var result = compositeTypeRenderer.render(compositeType);

      final var expectedOutput =
          """
          	entity "money_amount" <<composite>> {
          		amount : numeric(10,2)
          		currency_code : character(3)
          	}
          """;

      assertEquals(expectedOutput, result);
    }
  }
}
