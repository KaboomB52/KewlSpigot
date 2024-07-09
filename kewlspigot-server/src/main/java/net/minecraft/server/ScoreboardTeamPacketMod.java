package net.minecraft.server;

import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;

public final class ScoreboardTeamPacketMod {

    private PacketPlayOutScoreboardTeam packet;

    public ScoreboardTeamPacketMod(String name,String prefix,String suffix,Collection<String> players,int mode) {

        this.packet = new PacketPlayOutScoreboardTeam();
        packet.a = (name);
        packet.h = (mode);               //this.setField(fieldParamInt,paramInt);
        if (mode == 0 || mode == 2) {
            packet.b = (name); //this.setField(fieldDisplayName,name);
            packet.c = (prefix);    //this.setField(fieldPrefix,prefix);
            packet.d = (suffix);         //this.setField(fieldSuffix,suffix);
            packet.i = (3);              //this.setField(fieldPackOption,3);

        }
        if (mode == 0) {
            this.addAll(players);
        }

    }

    public ScoreboardTeamPacketMod(String name, Collection<String> players, int mode) {

        this.packet = new PacketPlayOutScoreboardTeam();

        if (players == null) {
            players = new ArrayList<>();
        }
        packet.a = (name); //this.setField(fieldTeamName, name);
        packet.h = (mode); //this.setField(fieldParamInt, paramInt);


        this.addAll(players);
    }

    public void sendToPlayer(Player bukkitPlayer) {
        ((CraftPlayer) bukkitPlayer).getHandle().playerConnection.sendPacket(this.packet);
    }

    private void addAll(Collection<String> col) {

        try {
            packet.g.addAll(col);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}