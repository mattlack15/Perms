package me.gravitinos.perms.core;

public class PermsManager {
    public static PermsManager instance = null;

    private PermsImplementation implementation;

    public PermsManager(PermsImplementation implementation){
        instance = this;
        this.implementation = implementation;
    }

    public PermsImplementation getImplementation() {
        return implementation;
    }
}
