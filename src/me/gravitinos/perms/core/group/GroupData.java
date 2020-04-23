package me.gravitinos.perms.core.group;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.subject.SubjectData;
import org.jetbrains.annotations.NotNull;

public class GroupData extends SubjectData {
    public static final int SERVER_LOCAL = PermsManager.instance.getImplementation().getConfigSettings().getServerId();
    public static final int SERVER_GLOBAL = Context.SERVER_ALL;
    private static final String PREFIX = "prefix";
    private static final String SUFFIX = "suffix";
    private static final String CHAT_COLOUR = "chat_colour";
    private static final String DESCRIPTION = "description";
    private static final String SERVER_CONTEXT = "server_context";
    public static final String SERVER_CONTEXT_KEY = SERVER_CONTEXT;
    private static final String PRIORITY = "priority";

    public GroupData() {
    }

    public GroupData(SubjectData data) {
        super(data);
    }

    public void setExtraData(String key, String value) {
        this.setData("EXTRA_" + key, value);
    }

    public String getExtraData(String key) {
        return this.getData("EXTRA_" + key);
    }

    public String getDescription() {
        return this.getData(DESCRIPTION, "");
    }

    public void setDescription(String description) {
        this.setData(DESCRIPTION, description);
    }

    public String getPrefix() {
        return this.getData(PREFIX, "");
    }

    public void setPrefix(String prefix) {
        this.setData(PREFIX, prefix);
    }

    public String getSuffix() {
        return this.getData(SUFFIX, "");
    }

    public void setSuffix(String suffix) {
        this.setData(SUFFIX, suffix);
    }

    public String getChatColour() {
        return this.getData(CHAT_COLOUR, "");
    }

    public void setChatColour(String colour) {
        this.setData(CHAT_COLOUR, colour);
    }

    public int getServerContext() {
        String cont = this.getData(SERVER_CONTEXT);
        if (cont == null) {
            return SERVER_LOCAL;
        }
        return Integer.parseInt(cont);
    }

    public void setServerContext(@NotNull String context) {
        this.setData(SERVER_CONTEXT, context);
    }

    public int getPriority() {
        String priority = this.getData(PRIORITY);
        if (priority == null) {
            this.setPriority(0);
            return 0;
        }
        try {
            return Integer.parseInt(priority);
        } catch (Exception e) {
            this.setPriority(0);
            return 0;
        }
    }

    public void setPriority(int i) {
        this.setData(PRIORITY, Integer.toString(i));
    }
}