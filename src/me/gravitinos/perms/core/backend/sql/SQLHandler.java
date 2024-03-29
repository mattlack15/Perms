package me.gravitinos.perms.core.backend.sql;

import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.context.MutableContextSet;
import me.gravitinos.perms.core.ladders.RankLadder;
import me.gravitinos.perms.core.subject.*;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
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

    public DataSource getDataSource() {
        return this.dataSource;
    }

    @Override
    public CompletableFuture<Void> addSubject(Subject<?> subject) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()) {
                dao.addSubject(subject);
            } catch (Exception e) {
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
            try (SQLDao dao = getDao()) {
                future.complete(dao.getSubject(subjectId));
            } catch (Exception e) {
                e.printStackTrace();
                future.complete(null);
            }
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> updateSubject(Subject<?> subject) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()) {
                dao.removeSubject(subject.getSubjectId(), false);
                dao.addSubject(subject);
            } catch (Exception e) {
                e.printStackTrace();
                future.complete(null);
            }
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> removeSubject(UUID subjectId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()) {
                dao.removeSubject(subjectId, true);
            } catch (Exception e) {
                e.printStackTrace();
                future.complete(null);
            }
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> updateSubjectData(Subject<?> subject) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()) {
                dao.setSubjectData(subject.getSubjectId(), subject.getData(), subject.getType());
                future.complete(null);
            } catch (Exception e) {
                e.printStackTrace();
                future.complete(null);
            }
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<GenericSubjectData> getSubjectData(UUID subjectId) {
        CompletableFuture<GenericSubjectData> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()) {
                future.complete(dao.getSubjectData(subjectId));
            } catch (Exception e) {
                e.printStackTrace();
                future.complete(null);
            }
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> addOrUpdateSubjects(List<Subject<?>> subjects) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        AtomicInteger ops = new AtomicInteger(subjects.size()+1);
        Runnable checker = () -> {
            if(ops.decrementAndGet() <= 0)
                future.complete(null);
        };
        runAsync(() -> {
            try (SQLDao dao = getDao()) {
                dao.updateSubjectPermsAndInheritances(subjects);
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
            checker.run();
            return null;
        });
        subjects.forEach(s -> {
            try (SQLDao dao = getDao()) {
                dao.setSubjectData(s.getSubjectId(), s.getData(), s.getType());
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
            checker.run();
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> removeSubjects(List<UUID> subjects) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()) {
                dao.removeSubjects(subjects);
                future.complete(null);
            } catch (Exception e) {
                e.printStackTrace();
                future.complete(null);
            }
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> removeInheritances(Subject<?> subject, List<UUID> parents) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()) {
                ArrayList<CachedInheritance> inheritances = new ArrayList<>();
                parents.forEach(p -> inheritances.add(new CachedInheritance(subject.getSubjectId(), p, "GENERIC", "GENERIC", new MutableContextSet())));
                dao.removeInheritances(inheritances);
            } catch (Exception e) {
                e.printStackTrace();
            }
            future.complete(null);
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> addInheritances(List<Inheritance> inheritances) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()) {
                ArrayList<CachedInheritance> cachedInheritances = new ArrayList<>();
                inheritances.forEach(i -> cachedInheritances.add(i.toCachedInheritance()));
                dao.addInheritances(cachedInheritances);
                future.complete(null);
            } catch (Exception e) {
                e.printStackTrace();
                future.complete(null);
            }
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<List<CachedSubject>> getAllSubjectsOfType(String type) {
        CompletableFuture<List<CachedSubject>> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()) {
                future.complete(dao.getAllSubjectsOfType(type));
            } catch (Exception e) {
                e.printStackTrace();
                future.complete(null);
            }
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> clearAllData() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()) {
                dao.clearTables();
                future.complete(null);
            } catch (Exception ignored) {
                future.complete(null);
            }
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> clearSubjectsOfType(String type) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()) {
                dao.removeSubjectsOfType(type);
                future.complete(null);
            } catch (Exception e) {
                e.printStackTrace();
                future.complete(null);
            }
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<List<RankLadder>> getRankLadders() {
        return runAsync(() -> {
            try (SQLDao dao = getDao()) {
                return dao.getRankLadders();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<RankLadder> getRankLadder(UUID id) {
        return runAsync(() -> {
            try (SQLDao dao = getDao()) {
                return dao.getRankLadder(id);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> removeRankLadder(UUID id) {
        return runAsync(() -> {
            try (SQLDao dao = getDao()) {
                dao.removeRankLadder(id);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> addRankLadder(RankLadder ladder) {
        return runAsync(() -> {
            try (SQLDao dao = getDao()) {
                dao.addRankLadder(ladder);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> updateRankLadder(RankLadder ladder) {
        return this.addRankLadder(ladder);
    }

    @Override
    public CompletableFuture<Map<Integer, String>> getServerIndex() {
        CompletableFuture<Map<Integer, String>> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()) {
                future.complete(dao.getServerIndex());
            } catch (Exception e) {
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
            try (SQLDao dao = getDao()) {
                dao.putServerIndex(serverId, serverName);
                future.complete(null);
            } catch (Exception e) {
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
            try (SQLDao dao = getDao()) {
                dao.removeServerIndex(serverId);
                future.complete(null);
            } catch (Exception e) {
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

    @Override
    public boolean isRemote() {
        return true;
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

    public void setDataSource(DataSource source) {
        this.dataSource = source;
    }

    /**
     * Starts the setup procedure
     *
     * @return a future result
     */
    public CompletableFuture<Boolean> setup() {
        return runAsync(() -> {
            try (SQLDao dao = getDao()) {
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
        try (SQLDao dao = getDao()) {
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
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }


//    @Override
//    public CompletableFuture<Void> addPermissions(Subject<?> subject, ImmutablePermissionList list) {
//        CompletableFuture<Void> future = new CompletableFuture<>();
//        runAsync(() -> {
//            try (SQLDao dao = getDao()) {
//                dao.addPermissions(subject.getSubjectId(), list);
//                future.complete(null);
//            } catch (Exception e) {
//                e.printStackTrace();
//                future.complete(null);
//            }
//            return null;
//        });
//        return future;
//    }
//
//    @Override
//    public CompletableFuture<Void> removePermissions(Subject<?> subject, List<String> list) {
//        CompletableFuture<Void> future = new CompletableFuture<>();
//        runAsync(() -> {
//            try (SQLDao dao = getDao()) {
//                dao.removePermissions(subject.getSubjectId(), list);
//                future.complete(null);
//            } catch (Exception e) {
//                e.printStackTrace();
//                future.complete(null);
//            }
//            return null;
//        });
//        return future;
//    }
//
//    @Override
//    public CompletableFuture<Void> removePermissionsExact(Subject<?> subject, List<PPermission> list) {
//        CompletableFuture<Void> future = new CompletableFuture<>();
//        runAsync(() -> {
//            try (SQLDao dao = getDao()) {
//                ArrayList<UUID> ids = new ArrayList<>();
//                list.forEach(p -> ids.add(p.getPermissionIdentifier()));
//                dao.removePermissionsExact(ids);
//                future.complete(null);
//            } catch (Exception e) {
//                e.printStackTrace();m
//                future.complete(null);
//            }
//            return null;
//        });
//        return future;
//    }
//
    @Override
    public CompletableFuture<Void> addSubjects(List<Subject<?>> subjects) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            try (SQLDao dao = getDao()) {
                dao.addSubjects(subjects);
                future.complete(null);
            } catch (Exception e) {
                e.printStackTrace();
                future.complete(null);
            }
            return null;
        });
        return future;
    }
    //    @Override
//    public CompletableFuture<ImmutablePermissionList> getPermissions(UUID subjectId) {
//        CompletableFuture<ImmutablePermissionList> future = new CompletableFuture<>();
//        runAsync(() -> {
//            try (SQLDao dao = getDao()) {
//                future.complete(new ImmutablePermissionList(dao.getPermissions(subjectId)));
//            } catch (Exception e) {
//                e.printStackTrace();
//                future.complete(null);
//            }
//            return null;
//        });
//        return future;
//    }

//    @Override
//    public CompletableFuture<Void> updatePermissions(Subject<?> subject) {
//        CompletableFuture<Void> future = new CompletableFuture<>();
//        runAsync(() -> {
//            try (SQLDao dao = getDao()) {
//                dao.removeAllPermissions(subject.getSubjectId());
//                dao.addPermissions(subject.getSubjectId(), Subject.getPermissions(subject));
//                future.complete(null);
//            } catch (Exception e) {
//                e.printStackTrace();
//                future.complete(null);
//            }
//            return null;
//        });
//        return future;
//    }

//    @Override
//    public CompletableFuture<Void> addPermission(Subject<?> subject, PPermission permission) {
//        CompletableFuture<Void> future = new CompletableFuture<>();
//        runAsync(() -> {
//            try (SQLDao dao = getDao()) {
//                dao.addPermission(subject.getSubjectId(), permission);
//                future.complete(null);
//            } catch (Exception e) {
//                e.printStackTrace();
//                future.complete(null);
//            }
//            return null;
//        });
//        return future;
//    }
//
//    @Override
//    public CompletableFuture<Void> removePermission(Subject<?> subject, String permission) {
//        CompletableFuture<Void> future = new CompletableFuture<>();
//        runAsync(() -> {
//            try (SQLDao dao = getDao()) {
//                dao.removePermission(subject.getSubjectId(), permission);
//                future.complete(null);
//            } catch (Exception e) {
//                e.printStackTrace();
//                future.complete(null);
//            }
//            return null;
//        });
//        return future;
//    }
//
//    @Override
//    public CompletableFuture<Void> removePermissionExact(Subject<?> subject, PPermission permission) {
//        CompletableFuture<Void> future = new CompletableFuture<>();
//        runAsync(() -> {
//            try (SQLDao dao = getDao()) {
//                dao.removePermission(permission.getPermissionIdentifier());
//                future.complete(null);
//            } catch (Exception e) {
//                e.printStackTrace();
//                future.complete(null);
//            }
//            return null;
//        });
//        return future;
//    }
//
//    @Override
//    public CompletableFuture<List<CachedInheritance>> getInheritances(UUID subjectId) {
//        CompletableFuture<List<CachedInheritance>> future = new CompletableFuture<>();
//        runAsync(() -> {
//            try (SQLDao dao = getDao()) {
//                future.complete(dao.getInheritances(subjectId));
//            } catch (Exception e) {
//                e.printStackTrace();
//                future.complete(null);
//            }
//            return null;
//        });
//        return future;
//    }
//
//    @Override
//    public CompletableFuture<Void> updateInheritances(Subject<?> subject) {
//        CompletableFuture<Void> future = new CompletableFuture<>();
//        runAsync(() -> {
//            try (SQLDao dao = getDao()) {
//                dao.removeAllInheritances(subject.getSubjectId());
//                ArrayList<CachedInheritance> inheritances = new ArrayList<>();
//                Subject.getInheritances(subject).forEach((i -> inheritances.add(new CachedInheritance(i.getChild().getSubjectId(), i.getParent().getSubjectId(), i.getChild().getType(), i.getParent().getType(), i.getContext()))));
//                new UnsupportedOperationException().printStackTrace();
//                dao.addInheritances(inheritances);
//                future.complete(null);
//            } catch (Exception e) {
//                e.printStackTrace();
//                future.complete(null);
//            }
//            return null;
//        });
//        return future;
//    }
//
//    @Override
//    public CompletableFuture<Void> addInheritance(@NotNull CachedInheritance inheritance) {
//        CompletableFuture<Void> future = new CompletableFuture<>();
//        runAsync(() -> {
//            try (SQLDao dao = getDao()) {
//                dao.addInheritance(inheritance.getChild(), inheritance.getParent(), inheritance.getChildType(), inheritance.getParentType(), inheritance.getContext());
//                future.complete(null);
//            } catch (Exception e) {
//                e.printStackTrace();
//                future.complete(null);
//            }
//            return null;
//        });
//        return future;
//    }
//
//    @Override
//    public CompletableFuture<Void> removeInheritance(Subject<?> subjectIdentifier, UUID parent) {
//        CompletableFuture<Void> future = new CompletableFuture<>();
//        runAsync(() -> {
//            try (SQLDao dao = getDao()) {
//                dao.removeInheritance(subjectIdentifier.getSubjectId(), parent);
//                future.complete(null);
//            } catch (Exception e) {
//                e.printStackTrace();
//                future.complete(null);
//            }
//            return null;
//        });
//        return future;
//    }
}
