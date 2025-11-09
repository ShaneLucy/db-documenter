package db.documenter.internal.renderer.impl;

import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.renderer.api.PumlRenderer;

import java.util.List;

public class RelationshipRenderer  implements PumlRenderer<List<Table>> {

    @Override
    public String render(List<Table> tables) {
        StringBuilder sb = new StringBuilder();

        for (Table table : tables) {
            for (ForeignKey fk : table.foreignKeys()) {
                sb.append(String.format("%s::%s --> %s::%s : %s\n",
                        fk.sourceTable(),
                        fk.sourceColumn(),
                        fk.targetTable(),
                        fk.targetColumn(),
                        fk.name()));
            }
        }
        return sb.toString();
    }
}
