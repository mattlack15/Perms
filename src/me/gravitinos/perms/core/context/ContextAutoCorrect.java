package me.gravitinos.perms.core.context;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.gravitinos.perms.core.PermsManager;

import java.util.Arrays;
import java.util.List;


public abstract class ContextAutoCorrect {
    public static final List<ContextAutoCorrect> correctors = Arrays.asList(
            new ContextAutoCorrect() {
                @Override
                StrPair correct(String key, String value) {
                    return new StrPair(key.toLowerCase(), value);
                }
            }, new ContextAutoCorrect() {
                @Override
                StrPair correct(String key, String value) {
                    if (key.equals(Context.SERVER_IDENTIFIER)) {
                        try {
                            Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            if (PermsManager.instance.getCachedServerIndex().containsValue(value)) {
                                value = Integer.toString(PermsManager.instance.getServerId(value));
                            } else {
                                throw new IllegalArgumentException("Cannot set value for key SERVER_IDENTIFIER to a server name that does not exist! ("  + value + ")");
                            }
                        }
                    }
                    return new StrPair(key, value);
                }
            });

    public static StrPair autoCorrect(String key, String value) {
        StrPair pair = new StrPair(key, value);
        for(ContextAutoCorrect corrector : correctors){
            pair = corrector.correct(pair.getKey(), pair.getValue());
        }
        return pair;
    }

    public synchronized static void addCorrector(ContextAutoCorrect corrector){
        correctors.add(corrector);
    }

    abstract StrPair correct(String key, String value);

    @Getter
    @AllArgsConstructor
    public static class StrPair {
        private String key;
        private String value;
    }
}
