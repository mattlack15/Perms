package me.gravitinos.perms.core.config;

import java.util.ArrayList;

public interface PermsConfiguration {

    //Getters
    ArrayList<String> getHelpMsgs(int page);
    boolean helpMsgPageExists(int page);
    String getPrefix();
    ArrayList<String> getGodUsers();
    boolean isCaseSensitiveGroups();
    boolean isUsingSQL();
    String getSQLDatabase();
    String getSQLUsername();
    String getSQLPassword();
    String getSQLHost();
    String getServerName();
    String getDefaultGroup();

    //Setters
    void setHelpMsgs(int page, ArrayList<String> msgs);
    void setPrefix(String pref);
    void setSQLDatabase(String sqlDatabase);
    void setSQLUsername(String sqlUsername);
    void setSQLHost(String sqlHost);
    void setUsingSQL(boolean usingSQL);
    void setDefaultGroup(String groupName);



}
