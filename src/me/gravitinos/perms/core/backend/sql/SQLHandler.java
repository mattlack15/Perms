package me.gravitinos.perms.core.backend.sql;

import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.subject.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class SQLHandler extends DataManager {
    private DataSource dataSource;
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
            try (SQLDao dao = getDao()){
                dao.addSubject(subject);
            }catch(Exception e){
                e.printStackTrace();
            }
            future.complete(null);
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<CachedSubject> getSubject(UUID subjectId) {
        CompletableFuture<CachedSubject> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()){
                future.complete(dao.getSubject(subjectId));
            }catch (Exception e) {
                e.printStackTrace();
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
           try(SQLDao dao = getDao()){
               dao.removeSubject(subject.getSubjectId(), false);
               dao.addSubject(subject);
           }catch(Exception e){
               e.printStackTrace();
               future.complete(null);}
           return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> removeSubject(UUID subjectId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()){
                dao.removeSubject(subjectId, true);
            }catch(Exception e){
                e.printStackTrace();
                future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<ImmutablePermissionList> getPermissions(UUID subjectId) {
        CompletableFuture<ImmutablePermissionList> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()){
                future.complete(new ImmutablePermissionList(dao.getPermissions(subjectId)));
            }catch(Exception e){
                e.printStackTrace();
                future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> updatePermissions(Subject subject) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()){
                dao.removeAllPermissions(subject.getSubjectId());
                dao.addPermissions(subject.getSubjectId(), Subject.getPermissions(subject));
                future.complete(null);
            }catch(Exception e){
                e.printStackTrace();
                future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> addPermission(Subject subject, PPermission permission) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()){
                dao.addPermission(subject.getSubjectId(), permission);
                future.complete(null);
            }catch(Exception e){
                e.printStackTrace();
                future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> removePermission(Subject subject, String permission) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()){
                dao.removePermission(subject.getSubjectId(), permission);
                future.complete(null);
            }catch(Exception e){
                e.printStackTrace();
                future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> removePermissionExact(Subject subject, String permission, UUID permIdentifier) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()){
                dao.removePermission(permIdentifier);
                future.complete(null);
            }catch(Exception e){
                e.printStackTrace();
                future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<ArrayList<CachedInheritance>> getInheritances(UUID subjectId) {
        CompletableFuture<ArrayList<CachedInheritance>> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()){
                future.complete(dao.getInheritances(subjectId));
            }catch(Exception e){
                e.printStackTrace();
                future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> updateInheritances(Subject subject) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()){
                dao.removeAllInheritances(subject.getSubjectId());
                ArrayList<CachedInheritance> inheritances = new ArrayList<>();
                Subject.getInheritances(subject).forEach((i -> inheritances.add(new CachedInheritance(i.getChild().getSubjectId(), i.getParent().getSubjectId(), i.getChild().getType(), i.getParent().getType(), i.getContext()))));
                dao.addInheritances(inheritances);
                future.complete(null);
            } catch(Exception e){
                e.printStackTrace();
                future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> addInheritance(Subject subject, Subject parent, Context context) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()){
                dao.addInheritance(subject.getSubjectId(), parent.getSubjectId(), subject.getType(), parent.getType(), context);
                future.complete(null);
            }catch(Exception e){
                e.printStackTrace();
                future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> removeInheritance(Subject subjectIdentifier, UUID parent) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()){
                dao.removeInheritance(subjectIdentifier.getSubjectId(), parent);
                future.complete(null);
            }catch(Exception e){
                e.printStackTrace();
                future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> updateSubjectData(Subject subject) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()){
                dao.setSubjectData(subject.getSubjectId(), subject.getData(), subject.getType());
                future.complete(null);
            }catch(Exception e){
                e.printStackTrace();
                future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<GenericSubjectData> getSubjectData(UUID subjectId) {
        CompletableFuture<GenericSubjectData> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()){
                future.complete(dao.getSubjectData(subjectId));
            }catch(Exception e){
                e.printStackTrace();
                future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> addPermissions(Subject subject, ImmutablePermissionList list) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()){
                dao.addPermissions(subject.getSubjectId(), list);
                future.complete(null);
            }catch(Exception e){
                e.printStackTrace();
                future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> removePermissions(Subject subject, ArrayList<String> list) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()){
                dao.removePermissions(subject.getSubjectId(), list);
                future.complete(null);
            }catch(Exception e){
                e.printStackTrace();
                future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> removePermissionsExact(Subject subject, ArrayList<PPermission> list) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()){
                ArrayList<UUID> ids = new ArrayList<>();
                list.forEach(p -> ids.add(p.getPermissionIdentifier()));
                dao.removePermissionsExact(ids);
                future.complete(null);
            }catch(Exception e) {
                e.printStackTrace();
                future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> addSubjects(ArrayList<Subject> subjects) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()){
                dao.addSubjects(subjects);
                future.complete(null);
            }catch(Exception e){
                e.printStackTrace(); future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> removeSubjects(ArrayList<UUID> subjects) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()){
                dao.removeSubjects(subjects);
                future.complete(null);
            }catch(Exception e){
                e.printStackTrace();
                future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> removeInheritances(Subject subject, ArrayList<UUID> parents) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()){
                ArrayList<CachedInheritance> inheritances = new ArrayList<>();
                parents.forEach(p -> inheritances.add(new CachedInheritance(subject.getSubjectId(), p, "GENERIC", "GENERIC", Context.CONTEXT_ALL)));
                dao.removeInheritances(inheritances);
            }catch(Exception e){
                e.printStackTrace();
            }
            future.complete(null);
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> addInheritances(ArrayList<Inheritance> inheritances) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()){
                ArrayList<CachedInheritance> cachedInheritances = new ArrayList<>();
                inheritances.forEach(i -> cachedInheritances.add(i.toCachedInheritance()));
                dao.addInheritances(cachedInheritances);
                future.complete(null);
            }catch(Exception e){
                e.printStackTrace();
                future.complete(null);
            }
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<ArrayList<CachedSubject>> getAllSubjectsOfType(String type) {
        CompletableFuture<ArrayList<CachedSubject>> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()){
                future.complete(dao.getAllSubjectsOfType(type));
            }catch(Exception e){
                e.printStackTrace();
                future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> clearAllData() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()){
                dao.clearTables();
                future.complete(null);
            }catch(Exception ignored){future.complete(null);}
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> clearSubjectOfType(String type) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()){
                dao.removeSubjectsOfType(type);
                future.complete(null);
            }catch(Exception e){
                e.printStackTrace();
                future.complete(null);
            }
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Map<Integer, String>> getServerIndex() {
        CompletableFuture<Map<Integer, String>> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()){
                future.complete(dao.getServerIndex());
            }catch(Exception e){
                e.printStackTrace();
                future.complete(null);
            }
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> putServerIndex(int serverId, String serverName) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()){
                dao.putServerIndex(serverId, serverName);
                future.complete(null);
            }catch(Exception e){
                e.printStackTrace();
                future.complete(null);
            }
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> removeServerIndex(int serverId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()){
                dao.removeServerIndex(serverId);
                future.complete(null);
            }catch(Exception e){
                e.printStackTrace();
                future.complete(null);
            }
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Boolean> testBackendConnection() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        runAsync(() -> {
            try {
                Connection connection = getConnection();
                if (connection.isValid(500)) {
                    future.complete(true);
                } else {
                    future.complete(false);
                }
            } catch (Exception e) {
                future.complete(false);
            }
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
            return this.dataSource.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setDataSource(DataSource source){
        this.dataSource = source;
    }

    /**
     * Starts the setup procedure
     *
     * @return a future result
     */
    public CompletableFuture<Boolean> setup() {
        return runAsync(() -> {
            try (SQLDao dao = getDao()){
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
        synchronized (SQLDao.class) {
            try {
                heldDao.set(getDao());
                dao.holdOpen++;
                return op.get();
            } finally {
                if (dao != null) {
                    if (--dao.holdOpen == 0) {
                        heldDao.set(null);
                    }
                    try {
                        dao.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

}
