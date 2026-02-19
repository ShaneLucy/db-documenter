package db.documenter.internal.renderer.impl;

import db.documenter.internal.models.db.DbCompositeType;
import db.documenter.internal.renderer.api.PumlRenderer;

/**
 * Renders database composite types as PlantUML entity definitions.
 *
 * <p>This renderer generates PlantUML notation for custom database composite types (e.g.,
 * PostgreSQL {@code CREATE TYPE ... AS (...)} composite types). Composite types are rendered
 * without schema qualification since they are already within a schema package in the PlantUML
 * output.
 *
 * <p><b>PlantUML Output Format:</b>
 *
 * <pre>
 * entity "composite_name" &lt;&lt;composite&gt;&gt; {
 *   field_name : field_type
 *   another_field : another_type
 * }
 * </pre>
 *
 * <p><b>Comparison to Enums:</b> Unlike {@link EnumRenderer} which renders simple value lists,
 * composite types render structured field definitions with names and types separated by colons.
 *
 * @see DbCompositeType
 * @see EnumRenderer
 */
public final class CompositeTypeRenderer implements PumlRenderer<DbCompositeType> {

  @Override
  public String render(final DbCompositeType compositeType) {
    final var stringBuilder = new StringBuilder();
    stringBuilder.append(
        String.format("\tentity \"%s\" <<composite>> {%n", compositeType.typeName()));

    compositeType
        .fields()
        .forEach(
            field ->
                stringBuilder.append(
                    String.format("\t\t%s : %s%n", field.fieldName(), field.fieldType())));

    stringBuilder.append("\t}\n");
    return stringBuilder.toString();
  }
}
