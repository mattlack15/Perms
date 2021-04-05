package me.gravitinos.perms.core.converter;

public abstract class Converter {
    public abstract String getName();
    public abstract boolean test();
    public abstract boolean convert();
}
