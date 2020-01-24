package me.gravitinos.perms.core.user;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.subject.SubjectRef;

import java.util.ArrayList;
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

    /**
     * Loads a specific user from the data manager specified in the constructor
     *
     * @param id       The id of the user
     * @param username The username of the user -> in case the user is not in the data manager and userdata has to be created
     * @return A future containing whether the operation was successful (true) or not (false)
     */
    public CompletableFuture<Boolean> loadUser(UUID id, String username) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();

        PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
            if (this.isUserLoaded(id)) {
                result.complete(true);
                return;
            }

            try {
                CachedSubject cachedSubject = dataManager.getSubject(id.toString()).get();
                User user = new User(cachedSubject, (s) -> new SubjectRef(GroupManager.instance.getGroup(s)), this);
                this.addUser(user);

            } catch (Exception e) {
                result.complete(false);
                e.printStackTrace();
            }

            result.complete(true);

        });

        return result;
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

    public void addUser(User user){
        this.loadedUsers.add(user);
    }
}
