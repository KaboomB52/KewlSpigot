package net.minecraft.server;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import net.minecraft.server.ScoreboardTeamBase.EnumNameTagVisibility;

public class PacketPlayOutScoreboardTeam implements Packet<PacketListenerPlayOut> {
    public String a = "";
    public String b = "";
    public String c = "";
    public String d = "";
    public String e;
    public int f;
    public Collection<String> g;
    public int h;
    public int i;

    public PacketPlayOutScoreboardTeam() {
        this.e = EnumNameTagVisibility.ALWAYS.e;
        this.f = -1;
        this.g = Lists.newArrayList();
    }

    public PacketPlayOutScoreboardTeam(ScoreboardTeam var1, int var2) {
        this.e = EnumNameTagVisibility.ALWAYS.e;
        this.f = -1;
        this.g = Lists.newArrayList();
        this.a = var1.getName();
        this.h = var2;
        if (var2 == 0 || var2 == 2) {
            this.b = var1.getDisplayName();
            this.c = var1.getPrefix();
            this.d = var1.getSuffix();
            this.i = var1.packOptionData();
            this.e = var1.getNameTagVisibility().e;
            this.f = var1.l().b();
        }

        if (var2 == 0) {
            this.g.addAll(var1.getPlayerNameSet());
        }

    }

    public PacketPlayOutScoreboardTeam(ScoreboardTeam var1, Collection<String> var2, int var3) {
        this.e = EnumNameTagVisibility.ALWAYS.e;
        this.f = -1;
        this.g = Lists.newArrayList();
        if (var3 != 3 && var3 != 4) {
            throw new IllegalArgumentException("Method must be join or leave for player constructor");
        } else if (var2 != null && !var2.isEmpty()) {
            this.h = var3;
            this.a = var1.getName();
            this.g.addAll(var2);
        } else {
            throw new IllegalArgumentException("Players cannot be null/empty");
        }
    }

    public void a(PacketDataSerializer var1) throws IOException {
        this.a = var1.c(16);
        this.h = var1.readByte();
        if (this.h == 0 || this.h == 2) {
            this.b = var1.c(32);
            this.c = var1.c(16);
            this.d = var1.c(16);
            this.i = var1.readByte();
            this.e = var1.c(32);
            this.f = var1.readByte();
        }

        if (this.h == 0 || this.h == 3 || this.h == 4) {
            int var2 = var1.e();

            for(int var3 = 0; var3 < var2; ++var3) {
                this.g.add(var1.c(40));
            }
        }

    }

    public void b(PacketDataSerializer var1) throws IOException {
        var1.a(this.a);
        var1.writeByte(this.h);
        if (this.h == 0 || this.h == 2) {
            var1.a(this.b);
            var1.a(this.c);
            var1.a(this.d);
            var1.writeByte(this.i);
            var1.a(this.e);
            var1.writeByte(this.f);
        }

        if (this.h == 0 || this.h == 3 || this.h == 4) {
            var1.b(this.g.size());
            Iterator var2 = this.g.iterator();

            while(var2.hasNext()) {
                String var3 = (String)var2.next();
                var1.a(var3);
            }
        }

    }

    public void a(PacketListenerPlayOut var1) {
        var1.a(this);
    }
}
