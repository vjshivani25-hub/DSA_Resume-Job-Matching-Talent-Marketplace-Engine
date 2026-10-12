package service;

import java.util.*;

public class Json {
    public static String esc(String s){
        if(s==null)return "";
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n"," ").replace("\r"," ");
    }
    public static String quote(String s){return "\""+esc(s)+"\"";}
    public static String arr(Collection<String> xs){
        StringBuilder b=new StringBuilder("["); int i=0;
        for(String x:xs){if(i++>0)b.append(",");b.append(quote(x));}
        return b.append("]").toString();
    }
    public static String number(double x){return String.format(Locale.US,"%.2f",x);}
}
