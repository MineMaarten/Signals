package com.minemaarten.signals.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class SignalsUtils {
    /** this method takes one very long string, and cuts it into lines which have
    a maxCharPerLine and returns it in a String list.
    it also preserves color formats. \n can be used to force a carriage
    return.
    */
   public static List<String> convertStringIntoList(String text, int maxCharPerLine){
       StringTokenizer tok = new StringTokenizer(text, " ");
       StringBuilder output = new StringBuilder(text.length());
       List<String> textList = new ArrayList<String>();
       String color = "";
       int lineLen = 0;
       while(tok.hasMoreTokens()) {
           String word = tok.nextToken();
           if(word.contains("\u00a7")) {// if there is a text formatter
                                        // present.
               for(int i = 0; i < word.length() - 1; i++)
                   if(word.substring(i, i + 2).contains("\u00a7")) color = word.substring(i, i + 2); // retrieve
                                                                                                     // the
                                                                                                     // color
                                                                                                     // format.
               lineLen -= 2;// don't count a color formatter with the line
                            // length.
           }
           if(lineLen + word.length() > maxCharPerLine || word.contains("\\n")) {
               word = word.replace("\\n", "");
               textList.add(output.toString());
               output.delete(0, output.length());
               output.append(color);
               lineLen = 0;
           } else if(lineLen > 0) {
               output.append(" ");
               lineLen++;
           }
           output.append(word);
           lineLen += word.length();
       }
       textList.add(output.toString());
       return textList;
   }
}
