package me.gravitinos.perms.core.util;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ObjectDeserializationException extends RuntimeException {

    public enum DeserializationExceptionCause {
        NO_DESERIALIZATION_METHOD,
        CLASS_NOT_FOUND
    }

    private DeserializationExceptionCause cause;
    public ObjectDeserializationException(String message, Throwable cause1, DeserializationExceptionCause cause) {
        super(message, cause1);
        this.cause = cause;
    }

    public DeserializationExceptionCause getDeserializationCause() {
        return cause;
    }
}
