package me.gravitinos.perms.spigot.util;

public class StringUtil {
    public static int indexOfIgnoreCase(String str, String subStr){
        int subIndex = 0;
        int currentIndex = 0;
        for(int i = 0; i < str.length(); i++){
            if(Character.toString(str.charAt(i)).toLowerCase().equals(Character.toString(subStr.charAt(subIndex)).toLowerCase())){
                if(subIndex == 0){
                    currentIndex = i;
                }
                subIndex++;
            } else if(subIndex > 0) {
                subIndex = 0;
                i--;
            }
            if(subIndex >= subStr.length()){
                return currentIndex;
            }
        }
        return -1;
    }
}
