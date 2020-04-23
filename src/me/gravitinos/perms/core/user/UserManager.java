package me.gravitinos.perms.core.user;

import com.google.common.collect.Lists;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.subject.Inheritance;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.core.subject.Subject;
import me.gravitinos.perms.core.subject.SubjectRef;
import me.gravitinos.perms.spigot.SpigotPerms;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class UserManager {

    public static UserManager instance;

    private DataManager dataManager;

    private ArrayList<User> loadedUsers = new ArrayList<>();

    public UserManager(DataManager dataManager) {
        instance = this;
        this.dataManager = dataManager;
    }

    public DataManager getDataManager() {
        return this.dataManager;
    }

    private static final Map<UUID, CompletableFuture<Boolean>> beingLoaded = new HashMap<>();

    /**
     * Loads a specific user from the data manager specified in the constructor
     *
     * @param id       The id of the user
     * @param username The username of the user -> in case the user is not in the data manager and userdata has to be created
     * @return A future containing whether the operation was successful (true) or not (false)
     */
    public synchronized CompletableFuture<Boolean> loadUser(@NotNull UUID id, @NotNull String username) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();

        synchronized (beingLoaded) {
            if (beingLoaded.containsKey(id) && !beingLoaded.get(id).isDone()) {
                return beingLoaded.get(id);
            } else {
                beingLoaded.remove(id);
                beingLoaded.put(id, result);
            }
        }


        PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {

            try {
                CachedSubject cachedSubject = dataManager.getSubject(id).get();
                User user;
                if (cachedSubject == null || cachedSubject.getData() == null || cachedSubject.getSubjectId() == null) {
                    user = new UserBuilder(id, username).addInheritance(GroupManager.instance.getDefaultGroup(), Context.CONTEXT_SERVER_LOCAL).build();
                    this.unloadUser(id);
                    this.addUser(user); //This will also save the user to the backend
                } else {
                    user = new User(cachedSubject, (s) -> new SubjectRef(GroupManager.instance.getGroupExact(s)), this);
                    if (this.isUserLoaded(user.getUniqueID())) {
                        this.getUser(user.getUniqueID()).updateFromCachedSubject(cachedSubject, (s) -> new SubjectRef(GroupManager.instance.getGroupExact(s))); //Will not save user to the backend (Purposefully)
                    } else {
                        this.loadedUsers.add(user); //This will NOT save the user to the backend (Purposefully)
                    }
                }

                //TODO remove later

                Map<Integer, String> index = PermsManager.instance.getCachedServerIndex();

                user.getDataManager().performOrderedOpAsync(() -> {
                    for (int ints : index.keySet()) {
                        String name = index.get(ints);
                        String finalServer = Integer.toString(ints);

                        //inheritance
                        boolean a = false;
                        for (Inheritance inheritances : user.getInheritances()) {
                            if (inheritances.getContext().getServer().equals(name)) {
                                a = true;
                            }
                        }
                        if (a) {
                            PermsManager.instance.getImplementation().sendDebugMessage("Found old-formatted subject inheritances in &e" + user.getName() + "&7 changing to new identifier-serverName format!");
                            ArrayList<Inheritance> inheritances = user.getInheritances();
                            user.clearInheritances();
                            inheritances.forEach(i -> {
                                if (i.getContext().getServer().equals(name)) {
                                    Context.setContextServer(i.getContext(), finalServer);
                                }
                            });
                            user.addInheritances(inheritances);
                        }


                        //perms
                        a = false;
                        for (PPermission perms : user.getOwnPermissions()) {
                            if (perms.getContext().getServer().equals(name)) {
                                a = true;
                            }
                        }
                        if (a) {
                            PermsManager.instance.getImplementation().sendDebugMessage("Found old-formatted permissions in &e" + user.getName() + "&7 changing to new identifier-serverName format!");
                            ArrayList<PPermission> perms = user.getOwnPermissions().getPermissions();
                            user.removeOwnPermissions(perms);
                            ArrayList<PPermission> newPerms = new ArrayList<>();
                            perms.forEach(p -> {
                                if (p.getContext().getServer().equals(name)) {
                                    newPerms.add(new PPermission(p.getPermission(), new Context(finalServer, p.getContext().getWorldName(), p.getExpiry()), p.getPermissionIdentifier()));
                                } else {
                                    newPerms.add(p);
                                }
                            });
                            user.addOwnPermissions(newPerms);
                        }
                    }
                    return null;
                });


            } catch (Throwable e) {
                Bukkit.getLogger().severe("Problem loading user " + username + " Exception: " + e.getMessage());
                SpigotPerms.instance.getImpl().addToLog(e.getMessage());
                result.complete(false);
                beingLoaded.remove(id);
                Bukkit.getLogger().severe("-----------------------------------------");
                e.printStackTrace();
            }

            result.complete(true);
            beingLoaded.remove(id);

        });

        return result;
    }

    public CompletableFuture<Boolean> reloadUsers() {
        Map<UUID, String> loaded = new HashMap<>();
        for (User user : Lists.newArrayList(this.loadedUsers)) {
            loaded.put(user.getUniqueID(), user.getName());
            this.unloadUser(user.getUniqueID());
        }

        ArrayList<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (UUID ids : loaded.keySet()) {
            futures.add(this.loadUser(ids, loaded.get(ids)));
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
            for (CompletableFuture<Boolean> booleanCompletableFuture : futures) {
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

    public ArrayList<User> getLoadedUsers() {
        return Lists.newArrayList(this.loadedUsers);
    }

    public CompletableFuture<Void> saveTo(DataManager dataManager) {
        ArrayList<Subject> users = Lists.newArrayList(loadedUsers);
        return dataManager.addSubjects(users);
    }

    /**
     * Unload a user
     *
     * @param uuid The unique ID of the user to unload
     */
    public void unloadUser(UUID uuid) {
        loadedUsers.removeIf(u -> u.getUniqueID().equals(uuid));
    }

    /**
     * Checks if a user is loaded in this user manager
     *
     * @param uuid The user's uuid to check for
     * @return True if the user is loaded in this user manager, false otherwise
     */
    public boolean isUserLoaded(UUID uuid) {
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
    public User getUser(UUID uuid) {
        for (User u : loadedUsers) {
            if (u.getUniqueID().equals(uuid)) {
                return u;
            }
        }
        return null;
    }

    public User getUserFromName(String name) {
        for (User u : loadedUsers) {
            if (u.getName().equals(name)) {
                return u;
            }
        }
        return null;
    }

    public CompletableFuture<Void> removeUser(User user) {
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
    public void addUser(User user) {
        this.loadedUsers.add(user);
        if (dataManager != null) {
            dataManager.addSubject(user);
        }
    }

    /**
     * Add a lot of users, PLEASE use this if adding a lot at once
     *
     * @param users List of the users to add
     */
    public void addUsers(ArrayList<User> users) {
        this.loadedUsers.addAll(users);
        ArrayList<Subject> subjects = Lists.newArrayList(users);
        if (dataManager != null) {
            dataManager.addSubjects(subjects);
        }
    }
}
