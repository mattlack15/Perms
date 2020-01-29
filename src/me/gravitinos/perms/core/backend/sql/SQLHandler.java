package me.gravitinos.perms.core.backend.sql;

import com.google.common.cache.Cache;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.subject.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class SQLHandler extends DataManager {
    private Connection connection;
    private String connectionURL = "";
    private ThreadLocal<SQLDao> heldDao = new ThreadLocal<>();
    private String password;
    private String username;

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
                future.complete(null);
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
           }catch(SQLException ignored){future.complete(null);}
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
            }catch(SQLException ignored){future.complete(null);}
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
            }catch(SQLException ignored){future.complete(null);}
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
            }catch(SQLException ignored){future.complete(null);}
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
            }catch(SQLException ignored){future.complete(null);}
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
            }catch(SQLException ignored){future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> removePermission(Subject subject, String permission, UUID permIdentifier) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try{
                getDao().removePermission(permIdentifier);
                future.complete(null);
            }catch(SQLException ignored){future.complete(null);}
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
            }catch(SQLException ignored){future.complete(null);}
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
            }catch(SQLException ignored){future.complete(null);}
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
            }catch(SQLException ignored){future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> removeInheritance(Subject subjectIdentifier, String parent) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try{
                getDao().removeInheritance(subjectIdentifier.getIdentifier(), parent);
                future.complete(null);
            }catch(SQLException ignored){future.complete(null);}
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
            }catch(SQLException ignored){future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<GenericSubjectData> getSubjectData(String subjectIdentifier) {
        CompletableFuture<GenericSubjectData> future = new CompletableFuture<>();
        runAsync(() -> {
            try{
                future.complete(getDao().getSubjectData(subjectIdentifier));
            }catch(SQLException ignored){future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> addPermissions(Subject subject, ImmutablePermissionList list) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try{
                getDao().addPermissions(subject.getIdentifier(), list);
                future.complete(null);
            }catch(SQLException ignored){future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> removePermissions(Subject subject, ArrayList<String> list) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try{
                getDao().removePermissions(subject.getIdentifier(), list);
                future.complete(null);
            }catch(SQLException ignored){future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> addSubjects(ArrayList<Subject> subjects) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try{
                getDao().addSubjects(subjects);
                future.complete(null);
            }catch(SQLException ignored){future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> removeSubjects(ArrayList<String> subjects) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try{
                getDao().removeSubjects(subjects);
                future.complete(null);
            }catch(SQLException ignored){future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> removeInheritances(Subject subjectIdentifier, ArrayList<String> parents) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try{
                ArrayList<CachedInheritance> inheritances = new ArrayList<>();
                parents.forEach(p -> inheritances.add(new CachedInheritance(subjectIdentifier.getIdentifier(), p, "GENERIC", "GENERIC", Context.CONTEXT_ALL)));
                getDao().removeInheritances(inheritances);
            }catch(SQLException ignored){}
            future.complete(null);
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> addInheritances(ArrayList<Inheritance> inheritances) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try{
                ArrayList<CachedInheritance> cachedInheritances = new ArrayList<>();
                inheritances.forEach(i -> cachedInheritances.add(i.toCachedInheritance()));
                getDao().addInheritances(cachedInheritances);
                future.complete(null);
            }catch(SQLException ignored){future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<ArrayList<CachedSubject>> getAllSubjectsOfType(String type) {
        CompletableFuture<ArrayList<CachedSubject>> future = new CompletableFuture<>();
        runAsync(() -> {
            try{
                SQLDao dao = getDao();
                future.complete(dao.getAllSubjectsOfType(type));
            }catch(SQLException ignored){future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> clearAllData() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try{
                getDao().clearTables();
                future.complete(null);
            }catch(SQLException ignored){future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> clearSubjectOfType(String type) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try{
                getDao().removeSubjectsOftype(type);
                future.complete(null);
            }catch(SQLException ignored){future.complete(null);}
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
                this.startConnection(connectionURL, username, password);
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
    public boolean startConnection(String connectionURL, String username, String password) throws SQLException {
        this.connectionURL = connectionURL;
        this.username = username;
        this.password = password;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            this.connection = DriverManager.getConnection(connectionURL, username, password);
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
