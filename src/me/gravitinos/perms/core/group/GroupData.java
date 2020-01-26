package me.gravitinos.perms.core.group;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.subject.SubjectData;
import org.jetbrains.annotations.NotNull;

public class GroupData extends SubjectData {
    private static final String PREFIX = "prefix";
    private static final String SUFFIX = "suffix";
    private static final String CHAT_COLOUR = "suffix";
    private static final String DESCRIPTION = "suffix";
    private static final String SERVER_CONTEXT = "server_context";

    public static final String SERVER_LOCAL = PermsManager.instance.getImplementation().getConfigSettings().getServerName();
    public static final String SERVER_GLOBAL = Context.VAL_ALL;

    public GroupData(){
        this.checkForServerContext();
    }

    public GroupData(SubjectData data){
        super(data);
        this.checkForServerContext();
    }

    public void setDescription(String description){
        this.setData(DESCRIPTION, description);
    }

    public String getDescription(){
        return this.getData(DESCRIPTION, "");
    }

    public void setPrefix(String prefix){
        this.setData(PREFIX, prefix);
    }
    public void setSuffix(String suffix){
        this.setData(SUFFIX, suffix);
    }
    public void setChatColour(String colour){
        this.setData(CHAT_COLOUR, colour);
    }

    public String getPrefix(){
        return this.getData(PREFIX, "");
    }

    public String getSuffix(){
        return this.getData(SUFFIX, "");
    }
    public String getChatColour(){
        return this.getData(CHAT_COLOUR, "");
    }

    public String getServerContext(){
        return this.getData(SERVER_CONTEXT);
    }

    public void setServerContext(@NotNull String context){
        this.setData(SERVER_CONTEXT, context);
        this.checkForServerContext();
    }

    private void checkForServerContext(){
        if(this.getServerContext() == null){
            this.setServerContext(SERVER_LOCAL);
        }
    }
}
