package me.gravitinos.perms.core.backend.sql;

import com.google.common.cache.Cache;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.subject.ImmutablePermissionList;
import me.gravitinos.perms.core.subject.Inheritance;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.core.subject.Subject;

import javax.annotation.concurrent.Immutable;
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
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try {
                SQLDao dao = getDao();
                dao.addSubject(subject);
            }catch(SQLException ignore){ }
            future.complete(null);
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<CachedSubject> getSubject(String name) {
        CompletableFuture<CachedSubject> future = new CompletableFuture<>();
        runAsync(() -> {
            try {
                future.complete(getDao().getSubject(name));
            }catch (SQLException ignored) {
            }
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> updateSubject(Subject subject) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
           try{
               SQLDao dao = getDao();
               dao.removeSubject(subject.getIdentifier());
               dao.addSubject(subject);
           }catch(SQLException ignored){}
           return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> removeSubject(String name) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try{
                getDao().removeSubject(name);
            }catch(SQLException ignored){}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<ImmutablePermissionList> getPermissions(String name) {
        CompletableFuture<ImmutablePermissionList> future = new CompletableFuture<>();
        runAsync(() -> {
            try{
                future.complete(new ImmutablePermissionList(getDao().getPermissions(name)));
            }catch(SQLException ignored){}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> updatePermissions(Subject subject) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try{
                SQLDao dao = getDao();
                dao.removeAllPermissions(subject.getIdentifier());
                dao.addPermissions(subject.getIdentifier(), Subject.getPermissions(subject));
                future.complete(null);
            }catch(SQLException ignored){}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> addPermission(Subject subject, PPermission permission) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try{
                getDao().addPermission(subject.getIdentifier(), permission);
                future.complete(null);
            }catch(SQLException ignored){}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> removePermission(Subject subject, String permission) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try{
                getDao().removePermission(subject.getIdentifier(), permission);
                future.complete(null);
            }catch(SQLException ignored){}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<ArrayList<CachedInheritance>> getInheritances(String name) {
        CompletableFuture<ArrayList<CachedInheritance>> future = new CompletableFuture<>();
        runAsync(() -> {
            try{
                future.complete(getDao().getInheritances(name));
            }catch(SQLException ignored){}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> updateInheritances(Subject subject) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try{
                SQLDao dao = getDao();
                dao.removeAllInheritances(subject.getIdentifier());
                ArrayList<CachedInheritance> inheritances = new ArrayList<>();
                Subject.getInheritances(subject).forEach((i -> inheritances.add(new CachedInheritance(i.getChild().getIdentifier(), i.getParent().getIdentifier(), i.getChild().getType(), i.getParent().getType(), i.getContext()))));
                dao.addInheritances(inheritances);
                future.complete(null);
            }catch(SQLException ignored){}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> addInheritance(Subject subject, Subject parent, Context context) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try{
                getDao().addInheritance(subject.getIdentifier(), parent.getIdentifier(), subject.getType(), parent.getType(), context);
                future.complete(null);
            }catch(SQLException ignored){}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> removeInheritance(String subjectIdentifier, String parent) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try{
                getDao().removeInheritance(subjectIdentifier, parent);
                future.complete(null);
            }catch(SQLException ignored){}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> updateSubjectData(Subject subject) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try{
                getDao().setSubjectData(subject.getIdentifier(), subject.getData(), subject.getType());
                future.complete(null);
            }catch(SQLException ignored){}
            return null;
        });
        return future;
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
    public CompletableFuture<Void> removeInheritances(String subjectIdentifier, ArrayList<String> parents) {
        return null;
    }

    @Override
    public CompletableFuture<Void> addInheritances(ArrayList<Inheritance> inheritances) {
        return null;
    }

    @Override
    public CompletableFuture<ArrayList<CachedSubject>> getAllSubjectsOfType(String type) {
        CompletableFuture<ArrayList<CachedSubject>> future = new CompletableFuture<>();
        runAsync(() -> {
            try{
                SQLDao dao = getDao();
                dao.subject
                future.complete(null);
            }catch(SQLException ignored){}
            return null;
        });
        return future;
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
