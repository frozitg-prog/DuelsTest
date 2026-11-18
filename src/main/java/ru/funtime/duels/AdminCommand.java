package ru.funtime.duels;

import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.Locale;

public final class AdminCommand implements CommandExecutor {
    private final FunTimeDuels plugin;private final ArenaManager arenas;private final KitManager kits;private final DuelManager duels;
    public AdminCommand(FunTimeDuels p,ArenaManager a,KitManager k,DuelManager d){plugin=p;arenas=a;kits=k;duels=d;}
    @Override public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args){
        if(!(sender instanceof Player p)){sender.sendMessage("Only players.");return true;}
        if(!p.isOp()){p.sendMessage("§cТолько OP может использовать эту команду.");return true;}
        if(args.length==0){help(p);return true;}
        if(args[0].equalsIgnoreCase("setspawn")){plugin.getConfig().set("spawn",p.getLocation());plugin.saveConfig();p.sendMessage("§aТочка возврата установлена.");return true;}
        if(args[0].equalsIgnoreCase("reload")){plugin.reloadConfig();p.sendMessage("§aКонфигурация перезагружена.");return true;}
        if(args[0].equalsIgnoreCase("kit")){
            if(args.length<3){p.sendMessage("§e/dueladmin kit create <name>");return true;}
            if(args[1].equalsIgnoreCase("create")){if(kits.create(args[2],p))p.sendMessage("§aКит "+args[2]+" создан из твоего инвентаря.");else p.sendMessage("§cТакой кит уже существует.");return true;}
        }
        if(args[0].equalsIgnoreCase("arena")){
            if(args.length<3){p.sendMessage("§e/dueladmin arena create|delete|pos1|pos2|setspawn1|setspawn2 <name>");return true;}
            String action=args[1].toLowerCase(Locale.ROOT),name=args[2].toLowerCase(Locale.ROOT);Location loc=p.getLocation();
            switch(action){
                case "create"->{arenas.create(name);p.sendMessage("§aАрена создана: "+name);}
                case "delete"->{arenas.delete(name);p.sendMessage("§aАрена удалена: "+name);}
                case "pos1","pos2","setspawn1","setspawn2"->{String key=switch(action){case "pos1"->"pos1";case "pos2"->"pos2";case "setspawn1"->"spawn1";default->"spawn2";};arenas.set(name,key,loc);p.sendMessage("§a"+key+" арены "+name+" установлен.");}
                default->help(p);
            }return true;
        }
        help(p);return true;
    }
    private void help(Player p){p.sendMessage("§6FunTimeDuels Admin");p.sendMessage("§e/dueladmin arena create <name>");p.sendMessage("§e/dueladmin arena pos1|pos2|setspawn1|setspawn2 <name>");p.sendMessage("§e/dueladmin arena delete <name>");p.sendMessage("§e/dueladmin kit create <name>");p.sendMessage("§e/dueladmin setspawn");p.sendMessage("§e/dueladmin reload");}
}