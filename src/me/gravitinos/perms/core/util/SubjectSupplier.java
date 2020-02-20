package me.gravitinos.perms.core.util;

import me.gravitinos.perms.core.subject.Subject;
import me.gravitinos.perms.core.subject.SubjectRef;

public interface SubjectSupplier {
    SubjectRef getSubject(String name);
}
