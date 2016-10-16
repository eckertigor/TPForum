package main.database.executor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by ekkert on 16.10.16.
 */
public class TExecutor {
    public static <T> T execQuery(Connection connection, String query, TResultHandler<T> handler) throws SQLException {
        T value;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(query);
            try (ResultSet result = stmt.getResultSet()) {
                value = handler.handle(result);
            }
        }

        return value;
    }

    public static void execQuery(Connection connection, String query) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(query);
        }
    }
}
