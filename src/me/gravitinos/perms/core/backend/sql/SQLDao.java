package me.gravitinos.perms.core.backend.sql;

import com.mysql.jdbc.Connection;

import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public abstract class SQLDao {

    int transactionCounter = 0;

    private Connection connection;

    public SQLDao(SQLHandler handler){

    }

    /**
     * Executes Something which in an SQL transaction (It ensure that either ALL the commands send to the SQL server are executed or none of them are)
     * @param func The function to run
     * @param <T> Return type
     * @return Whatever you set it to return
     * @throws SQLException Because this is using SQL
     */
    public <T> T executeInTransaction(Supplier<T> func) throws SQLException {
        transactionCounter++;
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try{
            T out = func.get();
            if(--transactionCounter <= 0){
                connection.commit();
            }
            return out;
        } finally {
            if(transactionCounter <= 0){
                connection.setAutoCommit(autoCommit);
            }
        }
    }
}
