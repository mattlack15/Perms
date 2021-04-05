package me.gravitinos.perms.core.util;

import me.gravitinos.perms.core.subject.Subject;
import me.gravitinos.perms.core.subject.SubjectRef;

import java.util.UUID;

public interface SubjectSupplier {
    SubjectRef getSubject(UUID subjectId);
}
