package ru.funtime.duels;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public final class DuelManager implements CommandExecutor, Listener {
    private final FunTimeDuels plugin; private final ArenaManager arenas; private final KitManager kits; private final StatsManager stats;
    private final Map<String,Deque<UUID>> queues=new HashMap<>();
    private final Map<UUID,Match> matches=new HashMap<>();
    private final Set<String> busyArenas=new HashSet<>();
    private final Map<UUID,Location> deathLocations=new HashMap<>();

    public DuelManager(FunTimeDuels p,ArenaManager a,KitManager k,StatsManager s){plugin=p;arenas=a;kits=k;stats=s;}
    public boolean isArenaBusy(String name){return busyArenas.contains(name.toLowerCase(Locale.ROOT));}
    public int queueCount(String kit){Deque<UUID> q=queues.get(kit.toLowerCase(Locale.ROOT));return q==null?0:q.size();}
    public boolean isInQueue(UUID playerId,String kit){Deque<UUID> q=queues.get(kit.toLowerCase(Locale.ROOT));return q!=null&&q.contains(playerId);}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(!(sender instanceof Player p)){sender.sendMessage("Only players can use this command.");return true;}
        if(args.length!=1){p.sendMessage("§eИспользование: /duel <kit>");p.sendMessage("§7Доступные киты: §f"+String.join(", ",kits.names()));return true;}
        String kit=args[0].toLowerCase(Locale.ROOT);
        if(!kits.exists(kit)){p.sendMessage("§cТакого кита нет.");return true;}
        if(matches.containsKey(p.getUniqueId())){p.sendMessage("§cТы уже участвуешь в дуэли.");return true;}
        Deque<UUID> q=queues.computeIfAbsent(kit,x->new ArrayDeque<>());
        if(q.remove(p.getUniqueId())){p.sendMessage("§eТы вышел из очереди §f"+kit+"§e.");if(q.isEmpty())queues.remove(kit);return true;}
        q.addLast(p.getUniqueId());p.sendMessage("§aТы встал в очередь §f"+kit+"§a. В очереди: §f"+q.size());
        tryStart(kit,q);return true;
    }
    private void tryStart(String kit,Deque<UUID> q){
        while(q.size()>=2){
            UUID a=q.peekFirst(),b=q.stream().skip(1).findFirst().orElse(null);
            Player p1=Bukkit.getPlayer(a),p2=Bukkit.getPlayer(b);
            if(p1==null||p2==null){q.remove(a);q.remove(b);continue;}
            String arena=arenas.nextArena();
            if(arena==null){p1.sendMessage("§cСейчас нет свободных арен. Ты остаёшься в очереди.");return;}
            q.remove(a);q.remove(b);if(q.isEmpty())queues.remove(kit);
            startMatch(p1,p2,kit,arena);
        }
    }
    private void startMatch(Player a,Player b,String kit,String arena){
        Match m=new Match(a,b,kit,arena);matches.put(a.getUniqueId(),m);matches.put(b.getUniqueId(),m);busyArenas.add(arena.toLowerCase(Locale.ROOT));
        m.save(a);m.save(b);kits.give(kit,a);kits.give(kit,b);
        a.setGameMode(GameMode.SURVIVAL);b.setGameMode(GameMode.SURVIVAL);
        a.teleport(arenas.get(arena,"spawn1"));b.teleport(arenas.get(arena,"spawn2"));
        a.sendMessage("§aПротивник найден: §f"+b.getName());b.sendMessage("§aПротивник найден: §f"+a.getName());
        a.sendTitle("§eОсмотр арены","§7У тебя 3 секунды",0,60,0);b.sendTitle("§eОсмотр арены","§7У тебя 3 секунды",0,60,0);
        new BukkitRunnable(){int t=3;public void run(){if(m.finished){cancel();return;}if(t>0){t--;return;}countdown(m);cancel();}}.runTaskTimer(plugin,20L,20L);
    }
    private void countdown(Match m){
        m.frozen=true;
        new BukkitRunnable(){int n=3;public void run(){
            if(m.finished){cancel();return;}
            if(n>0){title(m,"§e"+n,"§7Приготовься");n--;return;}
            title(m,"§aБОЙ!","§7Удачи!");m.frozen=false;m.started=true;cancel();
        }}.runTaskTimer(plugin,0L,20L);
    }
    private void title(Match m,String top,String sub){Player a=m.a(),b=m.b();if(a!=null)a.sendTitle(top,sub,0,20,0);if(b!=null)b.sendTitle(top,sub,0,20,0);}
    @EventHandler public void onMove(PlayerMoveEvent e){
        Match m=matches.get(e.getPlayer().getUniqueId());if(m==null||!m.frozen||e.getTo()==null)return;
        if(e.getFrom().getX()!=e.getTo().getX()||e.getFrom().getY()!=e.getTo().getY()||e.getFrom().getZ()!=e.getTo().getZ())e.setTo(e.getFrom());
    }
    @EventHandler public void onDamage(EntityDamageEvent e){
        if(!(e.getEntity() instanceof Player p))return;Match m=matches.get(p.getUniqueId());
        if(m!=null&&(!m.started||m.frozen||m.finished))e.setCancelled(true);
    }
    @EventHandler public void onDeath(PlayerDeathEvent e){
        Player loser=e.getEntity();Match m=matches.get(loser.getUniqueId());if(m==null)return;
        e.setKeepInventory(true);e.getDrops().clear();e.setDroppedExp(0);deathLocations.put(loser.getUniqueId(),loser.getLocation().clone());
        Player winner=m.other(loser);
        if(winner!=null)stats.record(winner.getUniqueId(),winner.getName(),true,true);
        stats.record(loser.getUniqueId(),loser.getName(),false,false);
        if(winner!=null){winner.sendMessage("§aТы победил в дуэли!");loser.sendMessage("§cТы проиграл дуэль.");}
        m.finished=true;
        Bukkit.getScheduler().runTaskLater(plugin,()->{
            if(loser.isOnline()){
                try{loser.spigot().respawn();}catch(Exception ignored){}
                loser.setGameMode(GameMode.SPECTATOR);
                Location at=deathLocations.get(loser.getUniqueId());if(at!=null)loser.teleport(at);
            }
        },1L);
        Bukkit.getScheduler().runTaskLater(plugin,()->endMatch(m),60L);
    }
    @EventHandler public void onRespawn(PlayerRespawnEvent e){
        Location at=deathLocations.get(e.getPlayer().getUniqueId());if(at!=null)e.setRespawnLocation(at);
        Match m=matches.get(e.getPlayer().getUniqueId());if(m!=null&&m.finished)Bukkit.getScheduler().runTask(plugin,()->e.getPlayer().setGameMode(GameMode.SPECTATOR));
    }
    private void endMatch(Match m){
        for(UUID id:List.of(m.aId,m.bId)){Player p=Bukkit.getPlayer(id);if(p!=null&&p.isOnline()){m.restore(p);p.teleport(plugin.getConfig().getLocation("spawn",p.getWorld().getSpawnLocation()));p.sendMessage("§eДуэль завершена.");}}
        matches.remove(m.aId);matches.remove(m.bId);busyArenas.remove(m.arena.toLowerCase(Locale.ROOT));deathLocations.remove(m.aId);deathLocations.remove(m.bId);
    }
    @EventHandler public void onQuit(PlayerQuitEvent e){
        UUID id=e.getPlayer().getUniqueId();
        for(Deque<UUID> q:queues.values())q.remove(id);
        Match m=matches.get(id);if(m!=null&&!m.finished){Player winner=m.other(e.getPlayer());if(winner!=null){stats.record(winner.getUniqueId(),winner.getName(),true,true);winner.sendMessage("§aПротивник вышел. Победа засчитана.");}stats.record(id,e.getPlayer().getName(),false,false);m.finished=true;Bukkit.getScheduler().runTaskLater(plugin,()->endMatch(m),60L);}
    }
    public void shutdown(){for(Match m:new HashSet<>(matches.values())){for(UUID id:List.of(m.aId,m.bId)){Player p=Bukkit.getPlayer(id);if(p!=null&&p.isOnline())m.restore(p);}}matches.clear();queues.clear();}
    private final class Match {
        final UUID aId,bId;final String kit,arena;final ItemStack[] aInv,bInv,aArm,bArm;final ItemStack aOff,bOff;final GameMode aMode,bMode;final double aHealth,bHealth;final int aFood,bFood;final float aExp,bExp;final int aLevel,bLevel;final Collection<PotionEffect> aEffects,bEffects;
        boolean frozen,started,finished;
        Match(Player a,Player b,String k,String ar){aId=a.getUniqueId();bId=b.getUniqueId();kit=k;arena=ar;aInv=copy(a.getInventory().getContents());bInv=copy(b.getInventory().getContents());aArm=copy(a.getInventory().getArmorContents());bArm=copy(b.getInventory().getArmorContents());aOff=clone(a.getInventory().getItemInOffHand());bOff=clone(b.getInventory().getItemInOffHand());aMode=a.getGameMode();bMode=b.getGameMode();aHealth=a.getHealth();bHealth=b.getHealth();aFood=a.getFoodLevel();bFood=b.getFoodLevel();aExp=a.getExp();bExp=b.getExp();aLevel=a.getLevel();bLevel=b.getLevel();aEffects=new ArrayList<>(a.getActivePotionEffects());bEffects=new ArrayList<>(b.getActivePotionEffects());}
        void save(Player p){}
        Player a(){return Bukkit.getPlayer(aId);}Player b(){return Bukkit.getPlayer(bId);}Player other(Player p){return p.getUniqueId().equals(aId)?b():a();}
        void restore(Player p){boolean first=p.getUniqueId().equals(aId);p.getInventory().clear();p.getInventory().setContents(copy(first?aInv:bInv));p.getInventory().setArmorContents(copy(first?aArm:bArm));p.getInventory().setItemInOffHand(clone(first?aOff:bOff));p.getActivePotionEffects().forEach(x->p.removePotionEffect(x.getType()));for(PotionEffect x:first?aEffects:bEffects)p.addPotionEffect(x);p.setGameMode(first?aMode:bMode);p.setFoodLevel(first?aFood:bFood);p.setExp(first?aExp:bExp);p.setLevel(first?aLevel:bLevel);p.setHealth(Math.max(1,Math.min(p.getMaxHealth(),first?aHealth:bHealth)));}
        ItemStack[] copy(ItemStack[] x){ItemStack[] y=new ItemStack[x.length];for(int i=0;i<x.length;i++)y[i]=clone(x[i]);return y;}
        ItemStack clone(ItemStack x){return x==null?null:x.clone();}
    }
}