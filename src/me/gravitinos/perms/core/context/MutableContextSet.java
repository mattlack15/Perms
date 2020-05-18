package me.gravitinos.perms.core.context;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Arrays;

public class MutableContextSet extends ContextSet {
    public MutableContextSet(Context... initial){
        super(initial);
    }
    public MutableContextSet(ContextSet initial){
        super(Lists.newArrayList(initial).toArray(new Context[0]));
        this.setExpiration(initial.getExpiration());
    }

    public void addContext(Context context){
        super.addContext(context);
    }

    public void removeContexts(String key){
        this.getContexts().removeIf(k -> k.getKey().equals(key));
    }

    public void removeContext(Context context){
        this.getContexts().removeIf(k -> k.equals(context));
    }

    @Override
    public ContextSet clone() {
        return new MutableContextSet(this.getContexts().toArray(new Context[0]));
    }
}
