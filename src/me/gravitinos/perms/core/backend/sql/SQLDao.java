package me.gravitinos.perms.core.backend.sql;

import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.subject.ImmutablePermissionList;
import me.gravitinos.perms.core.subject.PPermission;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.function.Supplier;

public class SQLDao {

    volatile int transactionCounter = 0;

    private Connection connection;

    private static final String TABLE_PERMISSIONS = "perms_permissions";
    private static final String TABLE_SUBJECTDATA = "perms_subjectdata";
    private static final String TABLE_INHERITANCE = "perms_inheritance";

    volatile int holdOpen = 0;

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
     *
     * @return String containing the statement
     */
    protected String getPermissionTableCreationUpdate() {
        return "CREATE TABLE IF NOT EXISTS " + TABLE_PERMISSIONS + " (OwnerIdentifier varchar(512), Permission varchar(512), Expiration varchar(256), Context varchar(1536))";
    }

    /**
     * Gets statement to create Subject Data table
     *
     * @return String containing the statement
     */
    protected String getSubjectDataTableCreationUpdate() {
        return "CREATE TABLE IF NOT EXISTS " + TABLE_SUBJECTDATA + " (Identifier varchar(512), Type varchar(64), Data varchar(10240))";
    }

    /**
     * Gets statement to create Inheritance table
     *
     * @return String containing the statement
     */
    protected String getInheritanceTableCreationUpdate() {
        return "CREATE TABLE IF NOT EXISTS " + TABLE_INHERITANCE + " (Child varchar(512), Parent varchar(512), ChildType varchar(64), ParentType varchar(64), Context varchar(1536))";
    }

    /**
     * Gets statement to query subject data from a specified identifier
     *
     * @return
     */
    protected String getSubjectDataFromIdentifierQuery() {
        return "SELECT * FROM " + TABLE_SUBJECTDATA + " WHERE Identifier=?";
    }

    /**
     * Gets the statement to query subject data from a type
     *
     * @return
     */
    protected String getSubjectDataFromTypeQuery() {
        return "SELECT * FROM " + TABLE_SUBJECTDATA + " WHERE Type=?";
    }

    /**
     * Gets the statement to delete subject data by some identifier
     *
     * @return
     */
    protected String getDeleteSubjectDataByIdentifierUpdate() {
        return "DELETE FROM " + TABLE_SUBJECTDATA + " WHERE Identifier=?";
    }

    /**
     * Gets the statement to delete subject data by some type
     *
     * @return
     */
    protected String getDeleteSubjectDataByTypeUpdate() {
        return "DELETE FROM " + TABLE_SUBJECTDATA + " WHERE Type=?";
    }

    /**
     * Gets the statement to insert subject data
     *
     * @return
     */
    protected String getInsertSubjectDataUpdate() {
        return "INSERT INTO " + TABLE_SUBJECTDATA + " (Identifier, Type, Data) VALUES (?, ?, ?)";
    }

    /**
     * Gets the statement to query inheritance from a specific child
     *
     * @return
     */
    protected String getInheritancesFromChildQuery() {
        return "SELECT * FROM " + TABLE_INHERITANCE + " WHERE Child=?";
    }

    /**
     * Gets the statement to delete inheritance by a child and parent
     *
     * @return
     */
    protected String getDeleteInheritanceByChildAndParentUpdate() {
        return "DELETE FROM " + TABLE_INHERITANCE + " WHERE Child=? AND PARENT=?";
    }

    /**
     * Gets the statement to delete inheritance by a child
     *
     * @return
     */
    protected String getDeleteInheritanceByChildUpdate() {
        return "DELETE FROM " + TABLE_INHERITANCE + " WHERE Child=?";
    }

    /**
     * Gets the statement to delete inheritance by a parent
     *
     * @return
     */
    protected String getDeleteInheritanceByParentUpdate() {
        return "DELETE FROM " + TABLE_INHERITANCE + " WHERE Parent=?";
    }

    /**
     * Gets the statement to delete inheritance by some child or parent
     *
     * @return
     */
    protected String getDeleteInheritanceByChildOrParentUpdate() {
        return "DELETE FROM " + TABLE_INHERITANCE + " WHERE Child=? OR Parent=?";
    }

    /**
     * Gets the statement to insert inheritance
     *
     * @return
     */
    protected String getInsertInheritanceUpdate() {
        return "INSERT INTO " + TABLE_INHERITANCE + " (Child, Parent, ChildType, ParentType, Context) VALUES (?, ?, ?, ?, ?)";
    }

    /**
     * Gets the statement to get permissions from an owner identifier
     *
     * @return
     */
    protected String getPermissionsFromOwnerIdentifierQuery() {
        return "SELECT * FROM " + TABLE_PERMISSIONS + " WHERE OwnerIdentifier=?";
    }

    /**
     * Gets the statement to delete permissions by an owner identifier and a permission string
     *
     * @return
     */
    protected String getDeletePermissionByOwnerIdentifierAndPermissionUpdate() {
        return "DELETE FROM " + TABLE_PERMISSIONS + " WHERE OwnerIdentifier=? AND Permission=?";
    }

    /**
     * Gets the statement to delete permissions by an owner identifier
     *
     * @return
     */
    protected String getDeletePermissionByOwnerIdentifierUpdate() {
        return "DELETE FROM " + TABLE_PERMISSIONS + " WHERE OwnerIdentifier=?";
    }

    protected String getInsertPermissionUpdate() {
        return "INSERT INTO " + TABLE_PERMISSIONS + " (OwnerIdentifier, Permission, Expiration, Context) VALUES (?, ?, ?, ?)";
    }

    //

    /**
     * Prepares a statement
     * @param statement SQL String
     * @return Prepared Statement
     * @throws SQLException
     */
    protected PreparedStatement prepareStatement(String statement) throws SQLException {
        return connection.prepareStatement(statement);
    }

    //




        //Permissions

    public ArrayList<PPermission> getPermissions(String ownerIdentifier) throws SQLException {
        ArrayList<PPermission> perms = new ArrayList<>();

        PreparedStatement s = prepareStatement(this.getPermissionsFromOwnerIdentifierQuery());

        s.setString(1, ownerIdentifier);

        ResultSet r = s.executeQuery();

        while(r.next()){
            PPermission perm = new PPermission(r.getString("Permission"), Context.fromString(r.getString("Context")), Long.parseLong(r.getString("Expiration")));
            perms.add(perm);
        }

        return perms;
    }

    public void addPermissions(String ownerIdentifier, ImmutablePermissionList permissions) throws SQLException {
        PreparedStatement s = prepareStatement(this.getInsertPermissionUpdate());

        for (PPermission perm : permissions) {
            s.setString(1, ownerIdentifier);
            s.setString(2, perm.getPermission());
            s.setString(3, Long.toString(perm.getExpiry()));
            s.setString(4, perm.getContext().toString());

            s.addBatch();
        }

        s.executeUpdate();
    }

    public void addPermission(String ownerIdentifier, PPermission permission) throws SQLException {
        PreparedStatement s = prepareStatement(this.getInsertPermissionUpdate());

        s.setString(1, ownerIdentifier);
        s.setString(2, permission.getPermission());
        s.setString(3, Long.toString(permission.getExpiry()));
        s.setString(4, permission.getContext().toString());

        s.executeUpdate();
    }

    public void removePermissions(String ownerIdentifier, ArrayList<String> permissions) throws SQLException {
        PreparedStatement s = prepareStatement(this.getDeletePermissionByOwnerIdentifierAndPermissionUpdate());

        for (String perms : permissions) {
            s.setString(1, ownerIdentifier);
            s.setString(2, perms);

            s.addBatch();
        }

        s.executeUpdate();
    }

    public void removePermission(String ownerIdentifier, String permission) throws SQLException {
        PreparedStatement s = prepareStatement(this.getDeletePermissionByOwnerIdentifierAndPermissionUpdate());

        s.setString(1, ownerIdentifier);
        s.setString(2, permission);

        s.executeUpdate();
    }

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
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

}
