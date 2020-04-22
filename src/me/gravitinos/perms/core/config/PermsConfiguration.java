package me.gravitinos.perms.core.config;

import java.util.ArrayList;

public interface PermsConfiguration {
    //Getters
    String getHelpFormat();
    String getHelpHeader();
    String getHelpFooter();
    String getPrefix();
    ArrayList<String> getGodUsers();
    boolean isCaseSensitiveGroups();
    boolean isUsingSQL();
    String getSQLDatabase();
    String getSQLUsername();
    String getSQLPassword();
    int getSQLPort();
    String getSQLHost();
    String getServerName();
    String getDefaultGroup();
    boolean isUsingBuiltInChat();
    int getServerId();
    String getLocalDefaultGroup();

    //Setters
    void setServerName(String name);
    void setPrefix(String pref);
    void setSQLDatabase(String sqlDatabase);
    void setSQLUsername(String sqlUsername);
    void setSQLPort(int port);
    void setSQLHost(String sqlHost);
    void setUsingSQL(boolean usingSQL);
    void setDefaultGroup(String groupName);
    void setLocalDefaultGroup(String groupName);
}