package me.gravitinos.perms.core.user;

import me.gravitinos.perms.core.subject.Subject;

import java.util.UUID;

public class User extends Subject {
    private final UUID uniqueId;
    private final String name;

    public User(UserBuilder builder) {
        super(Subject.USER, builder.uuid.toString());
        uniqueId = builder.uuid;
        name = builder.name;
    }



}
