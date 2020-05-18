package me.gravitinos.perms.core.backend;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StorageCredentials {
    private String username;
    private String password;
    private String host;
    private int port;
}
