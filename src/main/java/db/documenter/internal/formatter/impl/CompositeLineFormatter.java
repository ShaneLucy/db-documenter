package db.documenter.internal.formatter.impl;

import db.documenter.internal.formatter.api.LineFormatter;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Table;

import java.util.LinkedList;
import java.util.List;

public class CompositeLineFormatter implements LineFormatter {

    private final List<LineFormatter> lineFormatters;

    public CompositeLineFormatter(final List<LineFormatter> lineFormatters){
        this.lineFormatters = lineFormatters;
    }

    @Override
    public String format(final Table table, final Column column, final String current) {
        String result = current;
        for (LineFormatter lineFormatter : lineFormatters) {
            result = lineFormatter.format(table, column, result);
        }
        return result;
    }
}
