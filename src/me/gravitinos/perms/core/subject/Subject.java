package me.gravitinos.perms.core.subject;

import com.google.common.collect.Lists;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.context.ContextSet;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.user.User;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A class that represents an object that can have permissions, inheritances, and customizable data objects (SubjectData)
 */
public abstract class Subject<T extends SubjectData> {

    //Types
    public static final String GROUP = "GROUP";
    public static final String USER = "USER";

    private String type;

    private UUID subjectId;

    private ArrayList<PPermission> ownPermissions = new ArrayList<>();
    private ArrayList<Inheritance> inherited = new ArrayList<>();

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
     * Sets this subject's own permissions
     *
     * @param ownPermissions List of permissions
     */
    protected synchronized void setOwnPermissions(@NotNull ArrayList<PPermission> ownPermissions) {
        this.ownPermissions = ownPermissions;
        this.ownPermissions.removeIf(Objects::isNull);
    }

    /**
     * Get the Subject ID
     * This will always be the same for the same subject no matter what object it is represented by
     */
    public UUID getSubjectId() {
        return this.subjectId;
    }

    protected void setSubjectId(UUID id) {
        this.subjectId = id;
    }

    /**
     * Util
     */
    protected synchronized void removeExpiredPerms() {
        ImmutablePermissionList list = new ImmutablePermissionList(ownPermissions);

        long ctm = System.currentTimeMillis();

        ArrayList<PPermission> remove = new ArrayList<>();

        for (PPermission permissions : list) {
            if (permissions.isExpired(ctm)) {
                remove.add(permissions);
            }
        }

        if (remove.size() > 0) {
            remove.forEach(this::removeOwnSubjectPermission); //Remove from object
            if (this.getDataManager() != null) {
                this.getDataManager().removePermissionsExact(this, remove); //Update backend
            }
        }

        this.ownPermissions.removeIf(Objects::isNull);
    }

    public static ArrayList<Inheritance> getInheritances(Subject<?> subject) {
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
    protected ImmutablePermissionList getPermissions() {
        this.removeExpiredPerms();
        return new ImmutablePermissionList(ownPermissions);
    }

    /**
     * Sets the internal data object for this subject
     */
    protected void setData(@NotNull T data) {
        this.data = data;
    }


    /**
     * Get a list of the inherited subjects and the inheritance contexts
     *
     * @return list of inheritances
     */
    protected ArrayList<Inheritance> getInheritances() {
        this.removeExpiredInheritances();
        this.inherited.removeIf((i) -> !i.isValid());
        return Lists.newArrayList(this.inherited);
    }

    public String getName() {
        return this.data.getName();
    }

    public void setName(String name) {
        this.data.setName(name);
    }

    /**
     * Util
     */
    protected synchronized void removeExpiredInheritances() {

        long ctm = System.currentTimeMillis();

        ArrayList<Inheritance> remove = new ArrayList<>();
        ArrayList<UUID> removeParents = new ArrayList<>();

        inherited.removeIf(i -> !i.isValid());

        for (Inheritance in : inherited) {
            if (in.getContext().isExpired(ctm)) {
                remove.add(in);
                removeParents.add(in.getParent().getSubjectId());
            }
        }

        if (remove.size() > 0) {
            remove.forEach(inherited::remove); //Remove from object
            if (this.getDataManager() != null) {
                this.getDataManager().removeInheritances(this, removeParents); //Update backend
            }
        }

        this.inherited.removeIf(Objects::isNull);
    }

    /**
     * Removes all inheritances where parent.equals(inheritance.getParent())
     *
     * @param parent The specified parent to remove inheritances to
     */
    protected synchronized void removeOwnSubjectInheritance(Subject<? extends SubjectData> parent) {
        this.removeExpiredInheritances();
        this.inherited.removeIf(i -> !i.isValid() || parent.equals(i.getParent()));
    }

    /**
     * Removes all permissions where permission.equals(ownPermission)
     *
     * @param permission The permission to remove
     */
    protected synchronized PPermission removeOwnSubjectPermission(@NotNull PPermission permission) {
        AtomicReference<PPermission> p = new AtomicReference<>();
        this.ownPermissions.removeIf(Objects::isNull);
        ((ArrayList<PPermission>) this.ownPermissions.clone()).forEach(perm -> {
            if (perm.getPermissionIdentifier().equals(permission.getPermissionIdentifier())) {
                p.set(perm);
                ownPermissions.remove(perm);
            }
        });
        return p.get();
    }

    /**
     * Removes all permissions where p.getPermission().equalsIgnoreCase(permission)
     *
     * @param permission the permission to remove
     */
    protected synchronized void removeOwnSubjectPermission(@NotNull String permission) {
        this.ownPermissions.removeIf(p -> p.getPermission().equalsIgnoreCase(permission));
    }

    /**
     * Adds an inheritance to this subject
     *
     * @param subject The inheritance to add
     */
    protected synchronized void addOwnSubjectInheritance(Subject<?> subject, ContextSet context) {
        this.inherited.add(new Inheritance(new SubjectRef(subject), new SubjectRef(this), context));
    }

    /**
     * Adds an inheritance to this subject
     *
     * @param subject The inheritance to add
     */
    protected synchronized void addOwnSubjectInheritance(SubjectRef subject, ContextSet context) {
        this.inherited.add(new Inheritance(subject, new SubjectRef(this), context));
    }

    protected synchronized void addOwnInheritance(Inheritance inheritance) {
        this.inherited.add(inheritance);
    }


    /**
     * Adds a permission to this subject's own permission set
     *
     * @param permission the permission to add
     */
    protected synchronized void addOwnSubjectPermission(@NotNull PPermission permission) {
        this.ownPermissions.add(permission);
    }

    /**
     * Checks if this subject contains this exact permission, including checks for context and expiration
     *
     * @param permission
     * @return
     */
    protected boolean hasOwnPermission(PPermission permission) {
        return this.getPermissions().getPermissions().contains(permission);
    }

    /**
     * Checks if this subject contains this permission, excluding checks for context or expiration
     *
     * @param permission
     * @return
     */
    protected boolean hasOwnPermission(String permission, ContextSet context) {
        for (PPermission perms : this.getPermissions()) {
            if (perms.getPermission().equalsIgnoreCase(permission) && perms.getContext().isSatisfiedBy(context)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the type of subject this is
     *
     * @return Type
     */
    public String getType() {
        return type;
    }

    protected boolean hasOwnOrInheritedPermission(String permission, ContextSet contexts) {
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
    protected ArrayList<PPermission> getAllPermissions(ContextSet contexts) {

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
    protected ArrayList<PPermission> getAllPermissions() {
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
    public static void checkForAndRemoveInheritanceMistakes(List<Subject> subjects) {
        for (Subject subject : subjects) {
            treeSearchThing(subject, new ArrayList<>());
        }
    }

    private static void treeSearchThing(Subject<?> sub, ArrayList<Subject<?>> prev) {
        ArrayList<Subject<?>> subs = Lists.newArrayList(prev);
        subs.add(sub);

        for (Inheritance i : sub.getInheritances()) {
            Subject<?> subj = i.getParent();
            if (subs.contains(subj)) {
                if (sub instanceof User) {
                    ((User) sub).removeInheritance(subj);
                } else if (sub instanceof Group) {
                    ((Group) sub).removeInheritance(subj);
                } else {
                    sub.removeOwnSubjectInheritance(subj);
                }
                PermsManager.instance.getImplementation().addToLog(ChatColor.RED + "Mistake in inheritances, removed inheritance \"" +
                        subj.getName() + "\" from subject \"" + i.getChild().getName() + "\"");
            } else {
                treeSearchThing(subj, subs);
            }
        }
    }


}
