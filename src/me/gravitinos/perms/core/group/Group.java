package me.gravitinos.perms.core.group;

import me.gravitinos.perms.core.subject.Subject;

public class Group extends Subject {
    public Group(GroupBuilder builder) {
        super(builder.getName(), Subject.GROUP, builder.getData());
    }
}
