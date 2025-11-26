package db.documenter.internal.renderer.impl;

import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.renderer.api.PumlRenderer;

public class EnumRenderer implements PumlRenderer<DbEnum> {

  @Override
  public String render(final DbEnum dbEnum) {
    final var stringBuilder = new StringBuilder();
    stringBuilder.append(String.format("\tentity \"%s\" <<enum>> {%n", dbEnum.enumName()));

    dbEnum.enumValues().forEach(value -> stringBuilder.append(String.format("\t\t%s%n", value)));

    stringBuilder.append("\t}\n");
    return stringBuilder.toString();
  }
}
