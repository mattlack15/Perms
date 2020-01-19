package me.gravitinos.perms.core.backend.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Supplier;

public class SQLDao {

    int transactionCounter = 0;

    private Connection connection;

    private static final String TABLE_PERMISSIONS = "perms_permissions";
    private static final String TABLE_SUBJECTDATA = "perms_subjectdata";
    private static final String TABLE_INHERITANCE = "perms_inheritance";

    protected int holdOpen = 0;

    public SQLDao(SQLHandler handler) {
        this.connection = handler.getConnection();
    }

    /**
     * Executes Something which in an SQL transaction (It ensure that either ALL the commands send to the SQL server are executed or none of them are)
     *
     * @param func The function to run
     * @param <T>  Return type
     * @return Whatever you set it to return
     * @throws SQLException Because this is using SQL
     */
    public <T> T executeInTransaction(Supplier<T> func) throws SQLException {
        transactionCounter++;
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            T out = func.get();
            if (--transactionCounter <= 0) {
                connection.commit();
            }
            return out;
        } finally {
            if (transactionCounter <= 0) {
                connection.setAutoCommit(autoCommit);
            }
        }
    }

    //Statement getters

    /**
     * Gets statement to create permission table
     * @return String containing the statement
     */
    protected String getPermissionTableCreationUpdate() {
        return "CREATE TABLE IF NOT EXISTS " + TABLE_PERMISSIONS + " (Name varchar(512), Permission varchar(512), Expiration varchar(256), Context varchar(1536))";
    }

    /**
     * Gets statement to create Subject Data table
     * @return String containing the statement
     */
    protected String getSubjectDataTableCreationUpdate() {
        return "CREATE TABLE IF NOT EXISTS " + TABLE_SUBJECTDATA + " (Name varchar(512), Type varchar(64), Data varchar(2048))";
    }

    /**
     * Gets statement to create Inheritance table
     * @return String containing the statement
     */
    protected String getInheritanceTableCreationUpdate() {
        return "CREATE TABLE IF NOT EXISTS " + TABLE_INHERITANCE + " (Child varchar(512), Parent varchar(512), ChildType varchar(64), ParentType varchar(64), Context varchar(1536))";
    }

    //

    protected PreparedStatement prepareStatement(String statement) throws SQLException{
        return connection.prepareStatement(statement);
    }

    //
    public void initializeTables() throws SQLException {
        this.executeInTransaction(() -> {
            try {
                PreparedStatement s = this.prepareStatement(this.getInheritanceTableCreationUpdate());
                s.executeUpdate();
                s.close();
                s = this.prepareStatement(this.getPermissionTableCreationUpdate());
                s.executeUpdate();
                s.close();
                s = this.prepareStatement(this.getSubjectDataTableCreationUpdate());
                s.executeUpdate();
                s.close();
            } catch(SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

}
