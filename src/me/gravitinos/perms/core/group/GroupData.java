package me.gravitinos.perms.core.group;

import me.gravitinos.perms.core.subject.SubjectData;

public class GroupData extends SubjectData {
    private static final String PREFIX = "prefix";
    private static final String SUFFIX = "suffix";
    private static final String CHAT_COLOUR = "suffix";
    private static final String DESCRIPTION = "suffix";

    public GroupData(){}

    public GroupData(SubjectData data){
        super(data);
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
}
