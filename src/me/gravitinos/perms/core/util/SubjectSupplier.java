package me.gravitinos.perms.core.util;

import me.gravitinos.perms.core.subject.Subject;

public interface SubjectSupplier {
    Subject getSubject(String name);
}
