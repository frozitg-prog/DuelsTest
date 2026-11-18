package ru.funtime.duels;

import java.sql.*;
import java.util.UUID;

public final class StatsManager {
    private final Connection connection;
    public StatsManager(FunTimeDuels p) {
        Connection c=null;
        try { Class.forName("org.sqlite.JDBC"); c=DriverManager.getConnection("jdbc:sqlite:"+p.getDataFolder()+"/stats.db");
            try(Statement s=c.createStatement()){s.executeUpdate("CREATE TABLE IF NOT EXISTS stats (uuid TEXT PRIMARY KEY, name TEXT NOT NULL, wins INTEGER DEFAULT 0, losses INTEGER DEFAULT 0, kills INTEGER DEFAULT 0, deaths INTEGER DEFAULT 0, streak INTEGER DEFAULT 0)");}
        } catch(Exception e){p.getLogger().severe("Stats database: "+e.getMessage());}
        connection=c;
    }
    public synchronized void record(UUID id,String name,boolean win,boolean kill) {
        if(connection==null)return;
        try(PreparedStatement q=connection.prepareStatement("INSERT INTO stats(uuid,name,wins,losses,kills,deaths,streak) VALUES(?,?,?,?,?,?,?) ON CONFLICT(uuid) DO UPDATE SET name=excluded.name,wins=wins+excluded.wins,losses=losses+excluded.losses,kills=kills+excluded.kills,deaths=deaths+excluded.deaths,streak=CASE WHEN excluded.wins=1 THEN streak+1 ELSE 0 END")) {
            q.setString(1,id.toString());q.setString(2,name);q.setInt(3,win?1:0);q.setInt(4,win?0:1);q.setInt(5,kill?1:0);q.setInt(6,win?0:1);q.setInt(7,win?1:0);q.executeUpdate();
        } catch(SQLException e){e.printStackTrace();}
    }
    public synchronized int value(UUID id,String col) {
        if(connection==null)return 0;
        if(!java.util.Set.of("wins","losses","kills","deaths","streak").contains(col))return 0;
        try(PreparedStatement p=connection.prepareStatement("SELECT "+col+" FROM stats WHERE uuid=?")){p.setString(1,id.toString());try(ResultSet r=p.executeQuery()){return r.next()?r.getInt(1):0;}}catch(SQLException e){return 0;}
    }
    public synchronized String top(String col,int rank,boolean name) {
        if(connection==null)return name?"-":"0";
        if(!java.util.Set.of("wins","losses","kills","deaths","streak").contains(col))return "-";
        try(Statement s=connection.createStatement();ResultSet r=s.executeQuery("SELECT name,"+col+" FROM stats ORDER BY "+col+" DESC LIMIT "+Math.max(1,rank))){int i=0;while(r.next()){if(++i==rank)return name?r.getString(1):String.valueOf(r.getInt(2));}}catch(SQLException ignored){}
        return name?"-":"0";
    }
    public void close(){try{if(connection!=null)connection.close();}catch(SQLException ignored){}}
}