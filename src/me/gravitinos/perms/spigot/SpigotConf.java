package me.gravitinos.perms.spigot;

import me.gravitinos.perms.core.config.PermsConfiguration;

import java.util.ArrayList;

public class SpigotConf implements PermsConfiguration {
    @Override
    public ArrayList<String> getHelpMsgs(int page) {
        return null;
    }

    @Override
    public boolean helpMsgPageExists(int page) {
        return false;
    }

    @Override
    public String getPrefix() {
        return null;
    }

    @Override
    public ArrayList<String> getGodUsers() {
        return null;
    }

    @Override
    public boolean isCaseSensitiveGroups() {
        return false;
    }

    @Override
    public boolean isUsingSQL() {
        return false;
    }

    @Override
    public String getSQLDatabase() {
        return null;
    }

    @Override
    public String getSQLUsername() {
        return null;
    }

    @Override
    public String getSQLPassword() {
        return null;
    }

    @Override
    public String getSQLHost() {
        return null;
    }

    @Override
    public String getServerName() {
        return null;
    }

    @Override
    public void setHelpMsgs(int page, ArrayList<String> msgs) {

    }

    @Override
    public void setPrefix(String pref) {

    }
}
