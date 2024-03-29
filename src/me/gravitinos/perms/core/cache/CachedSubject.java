package me.gravitinos.perms.core.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.core.subject.SubjectData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Immutable
 */
@Getter
@AllArgsConstructor
public final class CachedSubject {
    private UUID subjectId;

    @Setter
    private String type;

    @Setter
    private SubjectData data;
    @Setter
    private List<PPermission> permissions;
    @Setter
    private List<CachedInheritance> inheritances;
}