package me.gravitinos.perms.core.context;

import lombok.Getter;
import lombok.Setter;
import me.gravitinos.perms.core.util.StringSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Set of contexts
 * NOTE on comparing: empty sets will be classified as global and will always satisfy all other context sets
 * NOTE on the above: it is groups into keys so an empty list filtered by a key will be seen as global
 */
public abstract class ContextSet implements Iterable<Context> {

    public static final int NO_EXPIRATION = -1;

    @Getter
    private List<Context> contexts = new ArrayList<>();

    @Getter
    @Setter
    private long expiration = NO_EXPIRATION;

    public ContextSet(Context... initial) {
        Arrays.asList(initial).forEach(this::addContext);
    }

    public boolean isExpired() {
        return isExpired(System.currentTimeMillis());
    }

    public boolean isExpired(long time) {
        return !(time < expiration || expiration == NO_EXPIRATION);
    }

    /**
     * Checks if each context in this has at least one match in the provided context
     * If the other context has 0 of a type, it will match with all of that type
     */
    public boolean isSatisfiedBy(ContextSet set) {

        if(this.size() == 0)
            return true;
        if(set.size() == 0)
            return false;

        List<String> keys = this.getUniqueKeys();
        for (String key : keys) {
            ContextSet filtered = set.filterByKey(key);
            if (filtered.size() == 0)
                return false;
            if (!filtered.containsAny(this.filterByKey(key)))
                return false;
        }
        return true;
    }

    public boolean oldSatisfiedBy(ContextSet set) {
        if (set.size() == 0 || this.size() == 0)
            return true;
        for (Context context : this) {
            ContextSet filtered = set.filterByKey(context.getKey());
            if (filtered.size() != 0 && !filtered.contains(context))
                return false;
        }
        return true;
    }

    public boolean containsAny(ContextSet set) {
        for (Context context : set)
            if (this.contains(context))
                return true;
        return false;
    }

    public List<String> getUniqueKeys() {
        List<String> keys = new ArrayList<>();
        this.contexts.forEach(c -> {
            if (!keys.contains(c.getKey()))
                keys.add(c.getKey());
        });
        return keys;
    }

    public int size() {
        return this.getContexts().size();
    }

    public boolean contains(Context context) {
        return this.getContexts().contains(context);
    }

    public boolean appliesToAny(Context context) {
        if (contexts.isEmpty())
            return true;
        boolean a = true;
        for (Context context1 : contexts) {
            if (!context1.getKey().equals(context.getKey())) {
                continue;
            }
            a = false;
            if (context1.equals(context))
                return true;
        }
        return a;
    }

    public MutableContextSet filterByKey(String key) {
        MutableContextSet set = new MutableContextSet();
        for (Context context : getContexts()) {
            if (context.getKey().equals(key))
                set.addContext(context);
        }
        return set;
    }

    public Context get(int i) {
        return this.contexts.get(i);
    }

    protected void addContext(Context context) {
        if (!this.contexts.contains(context))
            this.contexts.add(context);
    }

    public String toString() {
        StringSerializer serializer = new StringSerializer();
        serializer.writeLong(expiration);
        serializer.writeInt(contexts.size());
        contexts.forEach(c -> serializer.writeString(c.toString()));
        return serializer.toString();
    }

    public static ContextSet fromString(String str) {
        ContextSet set = new MutableContextSet();
        StringSerializer serializer = new StringSerializer(str);
        set.setExpiration(serializer.readLong());
        int amount = serializer.readInt();
        for (int i = 0; i < amount; i++)
            set.addContext(Context.fromString(serializer.readString()));
        return set;
    }

    @NotNull
    @Override
    public Iterator<Context> iterator() {
        return getContexts().iterator();
    }

    public abstract ContextSet clone();

    public ImmutableContextSet toImmutable() {
        return new ImmutableContextSet(this);
    }

    public MutableContextSet toMutable() {
        return new MutableContextSet(this);
    }

    /**
     * Basically returns whether either one could satisfy the other
     */
    public boolean canCollide(ContextSet context) {
        return context.isSatisfiedBy(this) || this.isSatisfiedBy(context);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ContextSet))
            return false;

        return ((ContextSet) o).contexts.containsAll(this.contexts) && this.contexts.containsAll(((ContextSet) o).contexts);
    }
}
