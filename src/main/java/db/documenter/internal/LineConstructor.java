package db.documenter.internal;

import db.documenter.internal.models.db.Table;

public class LineConstructor {


    public static String constructEntity(final Table table) {
        final var stringBuilder = new StringBuilder();

        stringBuilder.append(String.format("entity \"%s\" as %s {", table.name(), table.name()));
        stringBuilder.append("\n");
        table.columns().forEach(column -> {
            if (column.maximumLength() == 0) {
                stringBuilder.append(String.format("%s: %s",
                        column.name(),
                        column.dataType()));
            } else {
                stringBuilder.append(String.format("%s: %s(%s)",
                        column.name(),
                        column.dataType(),
                        column.maximumLength()));
            }

            stringBuilder.append("\n");
        });

        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
