package main.database.executor;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by ekkert on 16.10.16.
 */
public interface TResultHandler<T> {

    T handle(ResultSet resultSet) throws SQLException;

}