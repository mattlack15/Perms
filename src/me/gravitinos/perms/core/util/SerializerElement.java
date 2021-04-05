package me.gravitinos.perms.core.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SerializerElement {
    private Class<?> clazz;
    private Serializer<?> serializer;
}
