package me.gravitinos.perms.core.subject;

import com.google.common.collect.Lists;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.context.ContextSet;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * A class that represents an object that can have permissions, inheritances, and customizable data objects (SubjectData)
 */
public abstract class Subject<T extends SubjectData> {

    //Types
    public static final String GROUP = "GROUP";
    public static final String USER = "USER";

    private String type;

    private UUID subjectId;

    private final ConcurrentLoggedList<PPermission> ownPermissions = new ConcurrentLoggedList<>();
    private final ConcurrentLoggedList<Inheritance> inheritances = new ConcurrentLoggedList<>();

    @NotNull
    private T data;

    public Subject(@NotNull UUID subjectId, @NotNull String type, @NotNull T data) {
        this.type = type;
        this.data = data;
        this.subjectId = subjectId;
    }

    public abstract ContextSet getContext();

    public abstract DataManager getDataManager();

    @NotNull
    public T getData() {
        return this.data;
    }

    /**
     * Gets the type of subject this is
     *
     * @return Type
     */
    public synchronized String getType() {
        return type;
    }

    public ConcurrentLoggedList<PPermission> getOwnLoggedPermissions() {
        return this.ownPermissions;
    }

    public ConcurrentLoggedList<Inheritance> getOwnLoggedInheritances() {
        return this.inheritances;
    }

    /**
     * Schedule a save operation for this subject with this subject's data manager
     */
    public void queueSave() {
        if (getDataManager() != null)
            getDataManager().queueUpdate(this);
    }

    /**
     * Sets this subject's own permissions
     *
     * @param ownPermissions List of permissions
     */
    protected void setOwnPermissions(@NotNull List<PPermission> ownPermissions, boolean save) {
        if(save) {
            this.ownPermissions.set(ownPermissions, true);
            queueSave();
        } else {
            this.ownPermissions.set(ownPermissions, false);
        }
    }

    public void setPermissions(@NotNull List<PPermission> permissions) {
        setOwnPermissions(permissions, true);
    }

    /**
     * Get the Subject ID
     * This will always be the same for the same subject no matter what object it is represented by
     */
    public synchronized UUID getSubjectId() {
        return this.subjectId;
    }

    protected synchronized void setSubjectId(UUID id) {
        this.subjectId = id;
    }

    /**
     * Util
     */
    protected void removeExpiredPerms() {
        long ctm = System.currentTimeMillis();
        int size = this.ownPermissions.size();
        this.ownPermissions.removeIf((e) -> Objects.isNull(e) || e.isExpired(ctm));
        if(size != this.ownPermissions.size())
            queueSave();
    }

    public static List<Inheritance> getInheritances(Subject<?> subject) {
        return subject.getInheritances();
    }

    public static ImmutablePermissionList getPermissions(Subject<?> subject) {
        return subject.getPermissions();
    }


    /**
     * Get an immutable set of the permissions this subject contains
     *
     * @return Immutable set of permissions
     */
    public synchronized ImmutablePermissionList getPermissions() {
        this.removeExpiredPerms();
        return new ImmutablePermissionList(ownPermissions.get());
    }

    /**
     * Sets the internal data object for this subject
     */
    protected synchronized void setData(@NotNull T data) {
        this.data = data;
    }


    /**
     * Get a list of the inherited subjects and the inheritance contexts
     *
     * @return list of inheritances
     */
    public List<Inheritance> getInheritances() {
        this.removeExpiredInheritances();
        this.inheritances.removeIf((i) -> !i.isValid());
        return Lists.newArrayList(this.inheritances.get());
    }

    public String getName() {
        return this.data.getName();
    }

    public void setName(String name) {
        this.data.setName(name);
        queueSave();
    }

    /**
     * Util
     */
    protected void removeExpiredInheritances() {
        long ctm = System.currentTimeMillis();
        this.inheritances.removeIf((i) -> Objects.isNull(i) || !i.isValid() || i.getContext().isExpired(ctm));
    }

    public boolean hasInheritance(Subject<?> subject, ContextSet context) {
        for (Inheritance inheritance : getInheritances()) {
            if (subject.equals(inheritance.getParent()) && inheritance.getContext().equals(context)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether this has the parent/inheritance specified
     *
     * @param subject The parent/inheritance to check for
     * @return true if this subject has the specified inheritance
     */
    public boolean hasInheritance(@NotNull Subject<?> subject) {
        for (Inheritance inheritance : getInheritances()) {
            if (subject.equals(inheritance.getParent())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes all inheritances where parent.equals(inheritance.getParent())
     *
     * @param parent The specified parent to remove inheritances to
     */
    public void removeOwnSubjectInheritance(Subject<? extends SubjectData> parent) {
        this.removeExpiredInheritances();
        this.inheritances.removeIf(i -> !i.isValid() || parent.equals(i.getParent()));
        queueSave();
    }

    /**
     * Removes all permissions where permission.equals(ownPermission)
     *
     * @param permission The permission to remove
     * @return the last permission that matches the specified permission
     */
    public void removePermission(@NotNull PPermission permission) {
        this.ownPermissions.removeIf((perm) -> Objects.isNull(perm) || perm.getPermissionIdentifier().equals(permission.getPermissionIdentifier()));
        queueSave();
    }

    /**
     * Removes all permissions where p.getPermission().equalsIgnoreCase(permission)
     *
     * @param permission the permission to remove
     */
    public void removePermission(@NotNull String permission) {
        this.ownPermissions.removeIf(p -> p.getPermission().equalsIgnoreCase(permission));
        queueSave();
    }

    /**
     * Adds an inheritance to this subject
     *
     * @param subject The inheritance to add
     */
    public void addInheritance(Subject<?> subject, ContextSet context) {
        if(hasInheritance(subject, context))
            return;
        this.inheritances.weakAdd(new Inheritance(new SubjectRef(subject), new SubjectRef(this), context));
        queueSave();
    }

    /**
     * Adds an inheritance to this subject
     *
     * @param subject The inheritance to add
     */
    public synchronized void addInheritance(SubjectRef subject, ContextSet context) {
        this.inheritances.weakAdd(new Inheritance(subject, new SubjectRef(this), context));
        queueSave();
    }

    public synchronized void addInheritance(Inheritance inheritance) {
        this.inheritances.weakAdd(inheritance);
        queueSave();
    }


    /**
     * Adds a permission to this subject's own permission set
     *
     * @param permission the permission to add
     */
    public synchronized void addPermission(@NotNull PPermission permission) {
        this.ownPermissions.weakAdd(permission);
        queueSave();
    }

    /**
     * Checks if this subject contains this exact permission, including checks for context and expiration
     *
     * @param permission
     * @return
     */
    public synchronized boolean hasOwnPermission(PPermission permission) {
        return this.ownPermissions.contains(permission);
    }

    /**
     * Checks if this subject contains this permission, excluding checks for context or expiration
     *
     * @param permission
     * @return
     */
    public boolean hasOwnPermission(String permission, ContextSet context) {
        for (PPermission perms : this.ownPermissions.get()) {
            if (perms.getPermission().equalsIgnoreCase(permission) && perms.getContext().isSatisfiedBy(context)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasOwnOrInheritedPermission(String permission, ContextSet contexts) {
        if (this.hasOwnPermission(permission, contexts))
            return true;
        for (Inheritance inheritances : getInheritances()) {

            if (!inheritances.getContext().isSatisfiedBy(contexts) || inheritances.getParent().getContext().isSatisfiedBy(contexts))
                continue;

            if (inheritances.getParent().hasOwnOrInheritedPermission(permission, contexts))
                return true;
        }

        return false;
    }

    /**
     * Gets all permissions from this subject, including inherited permissions
     *
     * @return
     */
    public List<PPermission> getAllPermissions(ContextSet contexts) {

        ArrayList<PPermission> perms = new ArrayList<>();
        this.getPermissions().forEach(p -> {
            if (p.getContext().isSatisfiedBy(contexts))
                perms.add(p);
        });
        removeExpiredInheritances();

        for (Inheritance inheritances : getInheritances()) {

            if (!inheritances.getContext().isSatisfiedBy(contexts) || !inheritances.getParent().getContext().isSatisfiedBy(contexts))
                continue;

            //Add all the permissions to the perms array list
            perms.addAll(inheritances.getParent().getAllPermissions(contexts));
        }

        return perms;
    }

    /**
     * Gets all permissions from this subject, including inherited permissions
     *
     * @return
     */
    public List<PPermission> getAllPermissions() {
        ArrayList<PPermission> perms = new ArrayList<>(this.getPermissions().getPermissions());

        for (Inheritance inheritances : getInheritances()) {

            //Add all the permissions to the perms array list
            perms.addAll(inheritances.getParent().getAllPermissions());
        }

        return perms;
    }

    /**
     * Checks for, removes, and logs inheritance mistakes in a group of subjects
     *
     * @param subjects the group of subjects to check
     */
    public static void checkForAndRemoveInheritanceMistakes(List<Subject<?>> subjects) {
        for (Subject<?> subject : subjects) {
            inheritanceSearch(subject, new ArrayList<>());
        }
    }

    private static void inheritanceSearch(Subject<?> sub, List<Subject<?>> prev) {
        ArrayList<Subject<?>> subs = Lists.newArrayList(prev);
        subs.add(sub);

        for (Inheritance i : sub.getInheritances()) {
            Subject<?> subj = i.getParent();
            if (subs.contains(subj)) {
                sub.removeOwnSubjectInheritance(subj);
                PermsManager.instance.getImplementation().addToLog(ChatColor.RED + "Mistake in inheritances, removed inheritance \"" +
                        subj.getName() + "\" from subject \"" + i.getChild().getName() + "\"");
            } else {
                inheritanceSearch(subj, subs);
            }
        }
    }


}
