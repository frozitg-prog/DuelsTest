package ru.funtime.duels;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.File;
import java.util.*;

public final class ArenaManager {
    private final FunTimeDuels plugin;
    private final File file;
    private final YamlConfiguration cfg;
    public ArenaManager(FunTimeDuels plugin) {
        this.plugin=plugin; file=new File(plugin.getDataFolder(),"arenas.yml");
        if(!file.exists()) try { plugin.getDataFolder().mkdirs(); file.createNewFile(); } catch(Exception e){plugin.getLogger().severe(e.getMessage());}
        cfg=YamlConfiguration.loadConfiguration(file);
    }
    public void save() { try { cfg.save(file); } catch(Exception e){plugin.getLogger().severe("Cannot save arenas: "+e.getMessage());} }
    public void create(String name) { cfg.createSection("arenas."+name.toLowerCase(Locale.ROOT)); save(); }
    public void delete(String name) { cfg.set("arenas."+name.toLowerCase(Locale.ROOT),null); save(); }
    public void set(String name,String key,Location loc) {
        String p="arenas."+name.toLowerCase(Locale.ROOT)+"."+key;
        cfg.set(p+".world",loc.getWorld().getName()); cfg.set(p+".x",loc.getX()); cfg.set(p+".y",loc.getY()); cfg.set(p+".z",loc.getZ());
        cfg.set(p+".yaw",loc.getYaw()); cfg.set(p+".pitch",loc.getPitch()); save();
    }
    public Location get(String name,String key) {
        String p="arenas."+name+"."+key; String world=cfg.getString(p+".world");
        if(world==null || Bukkit.getWorld(world)==null) return null;
        return new Location(Bukkit.getWorld(world),cfg.getDouble(p+".x"),cfg.getDouble(p+".y"),cfg.getDouble(p+".z"),(float)cfg.getDouble(p+".yaw"),(float)cfg.getDouble(p+".pitch"));
    }
    public List<String> available() {
        List<String> out=new ArrayList<>(); ConfigurationSection s=cfg.getConfigurationSection("arenas");
        if(s!=null) for(String n:s.getKeys(false)) if(get(n,"spawn1")!=null && get(n,"spawn2")!=null) out.add(n);
        return out;
    }
    public String nextArena() {
        List<String> list=available(); Collections.shuffle(list);
        for(String n:list) if(!plugin.duels().isArenaBusy(n)) return n;
        return null;
    }
}