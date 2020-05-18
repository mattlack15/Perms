package me.gravitinos.perms.core.context;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ImmutableContextSet extends ContextSet {

    public ImmutableContextSet(ContextSet contextSet){
        this.setExpiration(contextSet.getExpiration());
        super.getContexts().addAll(contextSet.getContexts());
    }

    @Override
    public ContextSet clone() {
        return new ImmutableContextSet(this);
    }


    /**
     * Get a COPY of the list of contexts
     */
    @Override
    public List<Context> getContexts(){
        return new ArrayList<>(super.getContexts());
    }

}
