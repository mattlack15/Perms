package me.gravitinos.perms.core.util;

import java.util.ArrayList;

public class TextUtil {
    public static String[] splitIntoLines(String str, String regexWordSeperator, String lineBeginning, int maxLineSize){
        if(str.length() <= maxLineSize){
            return new String[]{str};
        }
        ArrayList<String> out = new ArrayList<>();
        String words[] = str.split( " ");
        String holder = lineBeginning + "";
        for(String word : words){
            if(holder.length() + word.length() >= maxLineSize){
                out.add(holder);
                holder = lineBeginning + word + " ";
            } else {
                holder += word + " ";
            }
        }
        out.add(holder);
        return out.toArray(new String[0]);
    }
    public static String[] splitIntoLines(String str, int maxLineSize){
        return TextUtil.splitIntoLines(str, " ", "", maxLineSize);
    }
}
