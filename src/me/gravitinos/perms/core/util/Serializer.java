package me.gravitinos.perms.core.util;

public interface Serializer<T> {
    void serialize(GravSerializer serializer, Object t);
    T deserialize(GravSerializer serializer, Object... args);
}
