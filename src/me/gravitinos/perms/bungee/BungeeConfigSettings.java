package me.gravitinos.perms.bungee;

import com.google.common.collect.Lists;
import me.gravitinos.perms.core.config.PermsConfiguration;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class BungeeConfigSettings implements PermsConfiguration {

    public static BungeeConfigSettings instance;

    private static final String PLUGIN_PREFIX = "plugin_prefix";
    private static final String GOD_USERS = "god_users";
    private static final String CASE_SENSITIVE_GROUPS = "case_sensitive_groups";
    private static final String SQL_ENABLED = "sql";
    private static final String SQL_HOST = "sql_host";
    private static final String SQL_PORT = "sql_port";
    private static final String SQL_DATABASE = "sql_database";
    private static final String SQL_TYPE = "database_type";
    private static final String SQL_USERNAME = "sql_username";
    private static final String SQL_PASSWORD = "sql_password";
    private static final String SERVER_NAME = "server_name";
    private static final String DEFAULT_GROUP = "default_group";
    private static final String CHAT_FORMAT = "chat_format";
    private static final String HELP_HEADER = "help_header";
    private static final String HELP_FOOTER = "help_footer";
    private static final String HELP_FORMAT = "help_format";
    private static final String SERVER_ID = "DO_NOT_CHANGE_server_id_DO_NOT_CHANGE";
    private static final String LOCAL_DEFAULT_GROUP = "local_default_group";

    public BungeeConfigSettings(){
        instance = this;
    }

    public ArrayList<String> getChatFormat(){
        return Lists.newArrayList(getConfig().getStringList(CHAT_FORMAT));
    }

    @Override
    public String getHelpFormat() {
        return getConfig().getString(HELP_FORMAT);
    }

    @Override
    public String getHelpHeader() {
        return getConfig().getString(HELP_HEADER);
    }

    @Override
    public String getHelpFooter() {
        return getConfig().getString(HELP_FOOTER);
    }

    @Override
    public String getPrefix() {
        return getConfig().getString(PLUGIN_PREFIX);
    }

    @Override
    public ArrayList<String> getGodUsers() {
        return Lists.newArrayList(getConfig().getStringList(GOD_USERS));
    }

    @Override
    public boolean isCaseSensitiveGroups() {
        return getConfig().getBoolean(CASE_SENSITIVE_GROUPS);
    }

    @Override
    public boolean isUsingSQL() {
        return getConfig().getBoolean(SQL_ENABLED);
    }

    @Override
    public String getSQLDatabase() {
        return getConfig().getString(SQL_DATABASE);
    }

    @Override
    public String getSQLUsername() {
        return getConfig().getString(SQL_USERNAME);
    }

    @Override
    public String getSQLPassword() {
        return getConfig().getString(SQL_PASSWORD);
    }

    @Override
    public String getDatabaseType() {
        return getConfig().getString(SQL_TYPE);
    }

    @Override
    public int getSQLPort() {
        return getConfig().getInt(SQL_PORT);
    }

    @Override
    public String getSQLHost() {
        return getConfig().getString(SQL_HOST);
    }

    @Override
    public String getServerName() {
        return getConfig().getString(SERVER_NAME);
    }

    @Override
    public String getDefaultGroup() {
        return getConfig().getString(DEFAULT_GROUP);
    }

    @Override
    public ArrayList<String> getLocalDefaultGroups() {
        return Lists.newArrayList(getConfig().getStringList(LOCAL_DEFAULT_GROUP));
    }

    @Override
    public boolean isUsingBuiltInChat() {
        return false;
    }

    @Override
    public int getServerId() {
        if(getConfig().getInt(SERVER_ID, -1) == -1){
            getConfig().set(SERVER_ID, new Random(System.currentTimeMillis() + Math.round(Math.random() * 1000)).nextInt(10000000));
            saveConfig();
        }
        return getConfig().getInt(SERVER_ID);
    }

    @Override
    public void setServerName(String name) {
        getConfig().set(SERVER_NAME, name);
        saveConfig();
    }

    private Configuration getConfig(){
        return BungeePerms.config;
    }

    private void saveConfig(){
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(getConfig(), new File(BungeePerms.instance.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setPrefix(String pref) {
        getConfig().set(PLUGIN_PREFIX, pref);
        saveConfig();
    }

    @Override
    public void setSQLDatabase(String sqlDatabase) {
        getConfig().set(SQL_DATABASE, sqlDatabase);
        saveConfig();
    }

    @Override
    public void setSQLUsername(String sqlUsername) {
        getConfig().set(SQL_USERNAME, sqlUsername);
        saveConfig();
    }

    @Override
    public void setSQLPort(int port) {
        getConfig().set(SQL_PORT, port);
        saveConfig();
    }

    @Override
    public void setSQLHost(String sqlHost) {
        getConfig().set(SQL_HOST, sqlHost);
        saveConfig();
    }

    @Override
    public void setUsingSQL(boolean usingSQL) {
        getConfig().set(SQL_ENABLED, usingSQL);
        saveConfig();
    }

    @Override
    public void setUsingBuiltInChat(boolean chat) {

    }

    @Override
    public void setDefaultGroup(String groupName) {
        getConfig().set(DEFAULT_GROUP, groupName);
        saveConfig();
    }

    @Override
    public void setLocalDefaultGroups(ArrayList<String> groupName) {
        getConfig().set(LOCAL_DEFAULT_GROUP, groupName);
        saveConfig();
    }
}
