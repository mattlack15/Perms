package me.gravitinos.perms.core.backend.sql;

import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.subject.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class SQLHandler extends DataManager {
    private DataSource dataSource;
    private Connection connection;
    private String connectionURL = "";

    @Override
    public CompletableFuture<Void> addSubject(Subject subject) {
        return null;
    }

    @Override
    public CompletableFuture<CachedSubject> getSubject(String name) {
        return null;
    }

    @Override
    public CompletableFuture<Void> updateSubject(Subject subject) {
        return null;
    }

    @Override
    public CompletableFuture<Void> removeSubject(String name) {
        return null;
    }

    @Override
    public CompletableFuture<ImmutablePermissionList> getPermissions(String name) {
        return null;
    }

    @Override
    public CompletableFuture<Void> updatePermissions(Subject subject) {
        return null;
    }

    @Override
    public CompletableFuture<Void> addPermission(Subject subject, PPermission permission) {
        return null;
    }

    @Override
    public CompletableFuture<Void> removePermission(Subject subject, String permission) {
        return null;
    }

    @Override
    public CompletableFuture<ArrayList<CachedInheritance>> getInheritances(String name) {
        return null;
    }

    @Override
    public CompletableFuture<Void> updateInheritances(Subject subject) {
        return null;
    }

    @Override
    public CompletableFuture<Void> addInheritance(Subject subject, Inheritance inheritance) {
        return null;
    }

    @Override
    public CompletableFuture<Void> removeInheritance(Subject subject, String parent) {
        return null;
    }

    @Override
    public CompletableFuture<Void> updateSubjectOption(Subject subject) {
        return null;
    }

    @Override
    public CompletableFuture<Void> addPermissionList(ImmutablePermissionList list) {
        return null;
    }

    @Override
    public CompletableFuture<Void> addSubjects(ArrayList<Subject> subjects) {
        return null;
    }

    @Override
    public CompletableFuture<Void> addInheritances(ArrayList<Inheritance> inheritances) {
        return null;
    }

    @Override
    public CompletableFuture<ArrayList<CachedSubject>> getAllSubjectsOfType(String type) {
        return null;
    }

    /**
     * Gets the connection from this sql handler
     * @return the connection object
     */
    public Connection getConnection(){
        try {
            if (this.connection == null || this.connection.isClosed()){
                this.startConnection(connectionURL);
            }
        } catch(SQLException ignored){ }
        return this.connection;
    }

    /**
     * Starts a connection between this SQL handler and the URL
     * @param connectionURL The url to use
     * @return Whether the connection was successful or not
     * @throws SQLException If this device does not support SQL
     */
    public boolean startConnection(String connectionURL) throws SQLException {
        this.connectionURL = connectionURL;
        try{
            Class.forName("com.mysql.jdbc.Driver");
            this.connection = DriverManager.getConnection(connectionURL);
        } catch(ClassNotFoundException e) {
            return false;
        }
        return true;
    }

    /**
     * Starts the setup procedure
     * @return a future result
     */
    public CompletableFuture<Boolean> setup(){

    }

}
