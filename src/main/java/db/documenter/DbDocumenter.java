package db.documenter;

import db.documenter.internal.db.api.ConnectionManager;
import db.documenter.internal.db.impl.PostgresConnectionManager;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Table;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static db.documenter.internal.LineConstructor.constructEntity;
import static db.documenter.internal.resultsets.Mapper.*;

public class DbDocumenter {

    private final ConnectionManager connectionManager;

    public DbDocumenter(final DbDocumenterConfig dbDocumenterConfig){
        this.connectionManager = new PostgresConnectionManager(dbDocumenterConfig, new Properties());
    }

    public String generatePUML() throws SQLException {


        try (final var connection = connectionManager.getConnection()){
            var statement = connection.createStatement();

            var tableResult = statement.executeQuery("SELECT * FROM information_schema.tables WHERE table_schema = 'public';");

            final List<Table> tables = new ArrayList<>();
            while (tableResult.next()) {
                tables.add(mapToTable(tableResult));
            }

            System.out.println("Discovered tables:");
            System.out.println(tables.size());

            final var tablesWithColumns = tables.stream().map(table -> {
                final List<Column> columns = new ArrayList<>();
                final Statement statement1;
                try {
                    statement1 = connection.createStatement();
                } catch (SQLException e) {
                    System.out.println(e);
                    throw new RuntimeException(e);
                }
                try {
                    final var columnResult = statement1.executeQuery(
                            "SELECT * FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'order_item'"
//                            String.format("SELECT * FROM information_schema.columns WHERE table_schema = '%' AND table_name = '%'",
//                                    table.schema(), table.name())
                    );

                    while (columnResult.next()) {
                        columns.add(mapToColumn(columnResult));
                    }


                    System.out.printf("For table %s detected %s columns%n", table.name(), columns.size());
                    return combineTableAndColumns(table, columns);
                } catch (SQLException e) {
                    System.out.println(e);
                    throw new RuntimeException(e);
                }
            }).toList();


            System.out.println(tablesWithColumns);

            System.out.println("\n\n\n");
            tablesWithColumns.forEach(table -> {
                final var tableToEntity = constructEntity(table);
                System.out.println(tableToEntity);
            });

            return "test";
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


    }
}
