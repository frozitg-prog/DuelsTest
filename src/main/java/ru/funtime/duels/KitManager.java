package ru.funtime.duels;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import java.io.*;
import java.util.*;

public final class KitManager {
    private final FunTimeDuels plugin; private final File file; private final YamlConfiguration cfg;
    public KitManager(FunTimeDuels p) { plugin=p; file=new File(p.getDataFolder(),"kits.yml"); if(!file.exists()) try{p.getDataFolder().mkdirs();file.createNewFile();}catch(Exception ignored){} cfg=YamlConfiguration.loadConfiguration(file); }
    public boolean create(String name, Player p) {
        String path="kits."+name.toLowerCase(Locale.ROOT);
        if(cfg.contains(path)) return false;
        cfg.set(path+".inventory", encode(p.getInventory().getContents()));
        cfg.set(path+".armor", encode(p.getInventory().getArmorContents()));
        cfg.set(path+".offhand", encode(new ItemStack[]{p.getInventory().getItemInOffHand()}));
        save(); return true;
    }
    public boolean exists(String n){return cfg.contains("kits."+n.toLowerCase(Locale.ROOT));}
    public Set<String> names(){ConfigurationSection s=cfg.getConfigurationSection("kits");return s==null?Collections.emptySet():s.getKeys(false);}
    public void give(String n,Player p) {
        String path="kits."+n.toLowerCase(Locale.ROOT);
        p.getInventory().clear(); p.getInventory().setContents(decode(cfg.getString(path+".inventory")));
        p.getInventory().setArmorContents(decode(cfg.getString(path+".armor")));
        ItemStack[] off=decode(cfg.getString(path+".offhand")); if(off.length>0)p.getInventory().setItemInOffHand(off[0]);
        p.updateInventory();
    }
    private void save(){try{cfg.save(file);}catch(IOException e){plugin.getLogger().severe(e.getMessage());}}
    private String encode(ItemStack[] items) {
        try { ByteArrayOutputStream b=new ByteArrayOutputStream(); BukkitObjectOutputStream o=new BukkitObjectOutputStream(b); o.writeInt(items.length); for(ItemStack i:items)o.writeObject(i);o.close();return Base64.getEncoder().encodeToString(b.toByteArray()); } catch(Exception e){throw new IllegalStateException(e);}
    }
    private ItemStack[] decode(String data) {
        if(data==null)return new ItemStack[0];
        try { BukkitObjectInputStream in=new BukkitObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(data))); int n=in.readInt(); ItemStack[] a=new ItemStack[n]; for(int i=0;i<n;i++)a[i]=(ItemStack)in.readObject();in.close();return a; } catch(Exception e){plugin.getLogger().warning("Cannot load kit: "+e.getMessage());return new ItemStack[0];}
    }
}