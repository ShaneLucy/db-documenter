package db.documenter.internal.renderer.impl;

import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.renderer.api.PumlRenderer;

/**
 * Renders database enum types as PlantUML entity definitions.
 *
 * <p>This renderer generates PlantUML notation for custom database enum types (e.g., PostgreSQL
 * {@code CREATE TYPE} enums). Enum names are rendered without schema qualification since they are
 * already within a schema package in the PlantUML output.
 *
 * <p><b>PlantUML Output Format:</b>
 *
 * <pre>
 * entity "enum_name" &lt;&lt;enum&gt;&gt; {
 *   VALUE_1
 *   VALUE_2
 *   VALUE_3
 * }
 * </pre>
 *
 * @see DbEnum
 */
public final class EnumRenderer implements PumlRenderer<DbEnum> {

  @Override
  public String render(final DbEnum dbEnum) {
    final var stringBuilder = new StringBuilder();
    stringBuilder.append(String.format("\tentity \"%s\" <<enum>> {%n", dbEnum.enumName()));

    dbEnum.enumValues().forEach(value -> stringBuilder.append(String.format("\t\t%s%n", value)));

    stringBuilder.append("\t}\n");
    return stringBuilder.toString();
  }
}
