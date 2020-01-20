package me.gravitinos.perms.core.backend.sql;

import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.subject.ImmutablePermissionList;
import me.gravitinos.perms.core.subject.Inheritance;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.core.subject.Subject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class SQLHandler extends DataManager {
    private Connection connection;
    private String connectionURL = "";
    private ThreadLocal<SQLDao> heldDao = new ThreadLocal<>();

    public SQLDao getDao() {
        if (heldDao.get() != null) {
            return heldDao.get();
        } else {
            return new SQLDao(this);
        }
    }

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
    public CompletableFuture<Void> removeInheritance(String subjectIdentifier, String parent) {
        return null;
    }

    @Override
    public CompletableFuture<Void> updateSubjectData(Subject subject) {
        return null;
    }

    @Override
    public CompletableFuture<Void> addPermissions(Subject subject, ImmutablePermissionList list) {
        return null;
    }

    @Override
    public CompletableFuture<Void> removePermissions(Subject subject, ImmutablePermissionList list) {
        return null;
    }

    @Override
    public CompletableFuture<Void> addSubjects(ArrayList<Subject> subjects) {
        return null;
    }

    @Override
    public CompletableFuture<Void> removeSubjects(ArrayList<String> subjects) {
        return null;
    }

    @Override
    public CompletableFuture<Void> removeInheritances(String subjectIdentifier, String parent) {
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
     *
     * @return the connection object
     */
    public Connection getConnection() {
        try {
            if (this.connection == null || this.connection.isClosed()) {
                this.startConnection(connectionURL);
            }
        } catch (SQLException ignored) {
        }
        return this.connection;
    }

    /**
     * Starts a connection between this SQL handler and the URL
     *
     * @param connectionURL The url to use
     * @return Whether the connection was successful or not
     * @throws SQLException If this device does not support SQL
     */
    public boolean startConnection(String connectionURL) throws SQLException {
        this.connectionURL = connectionURL;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            this.connection = DriverManager.getConnection(connectionURL);
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }

    /**
     * Starts the setup procedure
     *
     * @return a future result
     */
    public CompletableFuture<Boolean> setup() {
        return runAsync(() -> {
            try {
                SQLDao dao = getDao();
                try {
                    dao.initializeTables();
                    return true;
                } catch (Exception e) {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public <T> T performBulkOpSync(Supplier<T> op) {
        SQLDao dao = getDao();
        try {
            heldDao.set(getDao());
            dao.holdOpen++;
            return op.get();
        } finally {
            if (--dao.holdOpen == 0) {
                heldDao.set(null);
            }
        }

    }

    public <T> CompletableFuture<T> performOrderedOpAsync(Supplier<T> op) {
        return runAsync(() -> {
            boolean before = this.isKeepSync();
            this.setKeepSync(true);
            SQLDao dao = getDao();
            try {
                heldDao.set(getDao());
                dao.holdOpen++;
                T a = op.get();
                this.setKeepSync(before);
                return a;
            } finally {
                if (--dao.holdOpen == 0) {
                    heldDao.set(null);
                }
            }
        });
    }

}
