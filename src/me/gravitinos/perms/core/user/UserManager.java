package me.gravitinos.perms.core.user;

import com.google.common.collect.Lists;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.context.ContextSet;
import me.gravitinos.perms.core.context.MutableContextSet;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.subject.Subject;
import me.gravitinos.perms.core.subject.SubjectRef;
import me.gravitinos.perms.core.util.FutureIDLock;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class UserManager {

    public static UserManager instance;

    private DataManager dataManager;

    private ContextSet defaultGroupContext = new MutableContextSet(Context.CONTEXT_SERVER_LOCAL);

    private List<User> loadedUsers = Collections.synchronizedList(new ArrayList<>());

    private FutureIDLock<Boolean> loadLock = new FutureIDLock<>();

    public UserManager(DataManager dataManager) {
        instance = this;
        this.dataManager = dataManager;
    }

    public synchronized DataManager getDataManager() {
        return this.dataManager;
    }

    /**
     * Loads a specific user from the data manager specified in the constructor
     *
     * @param id       The id of the user
     * @param username The username of the user -> in case the user is not in the data manager and userdata has to be created
     * @return A future containing whether the operation was successful (true) or not (false)
     */
    public Future<Boolean> loadUser(@NotNull UUID id, @NotNull String username) {
        return loadUser(id, username, true);
    }

    /**
     * Loads a specific user from the data manager specified in the constructor
     *
     * @param id       The id of the user
     * @param username The username of the user -> in case the user is not in the data manager and userdata has to be created
     * @return A future containing whether the operation was successful (true) or not (false)
     */
    public Future<Boolean> loadUser(@NotNull UUID id, @NotNull String username, boolean addDefaultGroup) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();

        Future<Boolean> o = loadLock.tryLock(id, result);
        if (o != null) {
            return o;
        }

        PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {

            try {
                CachedSubject cachedSubject = dataManager.getSubject(id).get();
                User user;
                if (cachedSubject == null || cachedSubject.getData() == null || cachedSubject.getSubjectId() == null) {
                    if (addDefaultGroup) {
                        user = new UserBuilder(id, username).addInheritance(GroupManager.instance.getDefaultGroup(), getDefaultGroupInheritanceContext()).build();
                    } else {
                        user = new UserBuilder(id, username).build();
                    }
                    this.unloadUser(id);
                    this.addUser(user).get(); //This will also save the user to the backend
                } else {
                    user = new User(cachedSubject, (s) -> new SubjectRef(GroupManager.instance.getGroupExact(s)), this);
                    if (this.isUserLoaded(user.getUniqueID())) {
                        this.getUser(user.getUniqueID()).updateFromCachedSubject(cachedSubject, (s) -> new SubjectRef(GroupManager.instance.getGroupExact(s))); //Will not save user to the backend (Purposefully)
                    } else {
                        //Synchronized list so it's fine
                        this.loadedUsers.add(user); //This will NOT save the user to the backend (Purposefully)
                    }
                }
            } catch (Throwable e) {
                PermsManager.instance.getImplementation().addToLog("Problem loading user " + username + " (" + id + "): " + e.getMessage());
                e.printStackTrace();
                result.complete(false);
                return;
            } finally {
                loadLock.unlock(id);
            }
            result.complete(true);
        });

        return result;
    }

    public synchronized void setDefaultGroupInheritanceContext(ContextSet context) {
        this.defaultGroupContext = context;
    }

    public synchronized ContextSet getDefaultGroupInheritanceContext() {
        return this.defaultGroupContext;
    }

    public CompletableFuture<Boolean> reloadUsers() {
        Map<UUID, String> loaded = new HashMap<>();
        for (User user : getLoadedUsers()) {
            loaded.put(user.getUniqueID(), user.getName());
            this.unloadUser(user.getUniqueID());
        }

        ArrayList<Future<Boolean>> futures = new ArrayList<>();
        for (UUID ids : loaded.keySet()) {
            futures.add(this.loadUser(ids, loaded.get(ids)));
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
            for (Future<Boolean> booleanCompletableFuture : futures) {
                try {
                    booleanCompletableFuture.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            future.complete(true);
        });
        return future;
    }

    public synchronized ArrayList<User> getLoadedUsers() {
        return Lists.newArrayList(this.loadedUsers);
    }

    public synchronized CompletableFuture<Void> saveTo(DataManager dataManager) {
        ArrayList<Subject> users = Lists.newArrayList(loadedUsers);
        return dataManager.addSubjects(users);
    }

    /**
     * Unload a user
     *
     * @param uuid The unique ID of the user to unload
     */
    public synchronized void unloadUser(UUID uuid) {
        loadedUsers.removeIf(u -> u.getUniqueID().equals(uuid));
    }

    /**
     * Checks if a user is loaded in this user manager
     *
     * @param uuid The user's uuid to check for
     * @return True if the user is loaded in this user manager, false otherwise
     */
    public synchronized boolean isUserLoaded(UUID uuid) {
        for (User u : loadedUsers) {
            if (u.getUniqueID().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a user from this user manager
     *
     * @param uuid The uuid of the user
     * @return The user object or null if the user is not loaded in this user manager
     */
    public synchronized User getUser(UUID uuid) {
        for (User u : loadedUsers) {
            if (u.getUniqueID().equals(uuid)) {
                return u;
            }
        }
        return null;
    }

    public synchronized User getUserFromName(String name) {
        for (User u : loadedUsers) {
            if (u.getName().equals(name)) {
                return u;
            }
        }
        return null;
    }

    public synchronized CompletableFuture<Void> removeUser(User user) {
        if (user == null) {
            CompletableFuture<Void> f = new CompletableFuture<>();
            f.complete(null);
            return f;
        } else {
            this.unloadUser(user.getUniqueID());
            return this.dataManager.removeSubject(user.getSubjectId());
        }
    }

    /**
     * Add a user (Btw if adding a lot in a short time, please use addUsers
     *
     * @param user The user to add
     */
    public synchronized CompletableFuture<Void> addUser(User user) {
        if (user == null) {
            System.out.println("PERMS > TRIED TO ADD NULL USER TO USERMANAGER -> contact grav");
            new IllegalArgumentException("").printStackTrace();
        }
        this.loadedUsers.add(user);
        if (dataManager != null) {
            return dataManager.addSubject(user);
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }

    /**
     * Add a lot of users, PLEASE use this if adding a lot at once
     *
     * @param users List of the users to add
     */
    public synchronized void addUsers(ArrayList<User> users) {
        this.loadedUsers.addAll(users);
        ArrayList<Subject> subjects = Lists.newArrayList(users);
        if (dataManager != null) {
            dataManager.addSubjects(subjects);
        }
    }
}
