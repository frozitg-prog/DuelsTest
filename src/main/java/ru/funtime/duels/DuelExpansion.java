package ru.funtime.duels;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public final class DuelExpansion extends PlaceholderExpansion {
    private final FunTimeDuels plugin; private final StatsManager stats;
    public DuelExpansion(FunTimeDuels p,StatsManager s){plugin=p;stats=s;}
    @Override public String getIdentifier(){return "funtimeduels";}
    @Override public String getAuthor(){return "frozitg-prog";}
    @Override public String getVersion(){return plugin.getDescription().getVersion();}
    @Override public boolean persist(){return true;}
    @Override public String onRequest(OfflinePlayer p,String params){
        if(params.startsWith("top_")){String[] a=params.split("_");if(a.length==4)return stats.top(a[1],Integer.parseInt(a[2]),a[3].equalsIgnoreCase("name"));}
        if(params.startsWith("queue_"))return ""+plugin.duels().queueCount(params.substring(6));
        if(p==null)return "";
        return switch(params.toLowerCase()){case "wins"->""+stats.value(p.getUniqueId(),"wins");case "losses"->""+stats.value(p.getUniqueId(),"losses");case "kills"->""+stats.value(p.getUniqueId(),"kills");case "deaths"->""+stats.value(p.getUniqueId(),"deaths");case "streak"->""+stats.value(p.getUniqueId(),"streak");default->"";};
    }
}