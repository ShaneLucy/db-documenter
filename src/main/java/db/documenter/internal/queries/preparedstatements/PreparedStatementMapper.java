package db.documenter.internal.queries.preparedstatements;

import db.documenter.internal.models.db.Table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PreparedStatementMapper {




    public void prepareTableInfoStatement(final PreparedStatement  preparedStatement, final String schema) throws SQLException {
            preparedStatement.setString(1, schema);
    }

    public void prepareColumnInfoStatement(final PreparedStatement  preparedStatement, final String schema, final String tableName) throws SQLException{
        preparedStatement.setString(1, schema);
        preparedStatement.setString(2, tableName);
    }

    public void preparePrimaryKeyInfoStatement(final PreparedStatement  preparedStatement, final String schema, final Table table) throws SQLException {
        preparedStatement.setString(1, schema);
        preparedStatement.setString(2, table.name());

    }
    public void prepareForeignKeyInfoStatement(final PreparedStatement  preparedStatement, final String schema, final Table table) throws SQLException {
            preparedStatement.setString(1, schema);
            preparedStatement.setString(2, table.name());

    }


}
