package ru.funtime.duels;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class FunTimeDuels extends JavaPlugin {
    private ArenaManager arenas;
    private KitManager kits;
    private DuelManager duels;
    private StatsManager stats;

    @Override public void onEnable() {
        saveDefaultConfig();
        arenas = new ArenaManager(this);
        kits = new KitManager(this);
        stats = new StatsManager(this);
        duels = new DuelManager(this, arenas, kits, stats);
        getCommand("duel").setExecutor(duels);
        getCommand("dueladmin").setExecutor(new AdminCommand(this, arenas, kits, duels));
        Bukkit.getPluginManager().registerEvents(duels, this);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) new DuelExpansion(this, stats).register();
        getLogger().info("FunTimeDuels enabled.");
    }

    @Override public void onDisable() { if (duels != null) duels.shutdown(); if (stats != null) stats.close(); }
    public DuelManager duels() { return duels; }
}