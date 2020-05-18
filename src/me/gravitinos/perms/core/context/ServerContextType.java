package me.gravitinos.perms.core.context;

import lombok.Getter;

public enum ServerContextType {
        GLOBAL("&c&lGLOBAL"), LOCAL("&a&lLOCAL"), EXTRA_LOCAL("&6&lEXTRA_LOCAL"), FOREIGN("&6&lFOREIGN");
        @Getter
        private String display;
        ServerContextType(String display){
            this.display = display;
        }
        public static ServerContextType getType(ContextSet contexts){
            ContextSet servers = contexts.filterByKey(Context.SERVER_IDENTIFIER);
            if(servers.size() == 0)
                return ServerContextType.GLOBAL;
            if(servers.size() > 1)
                return ServerContextType.EXTRA_LOCAL;
            if(servers.getContexts().get(0).equals(Context.CONTEXT_SERVER_LOCAL))
                return ServerContextType.LOCAL;
            return ServerContextType.FOREIGN;
        }
    }