package org.eytril.spigot.knockback;

import org.bukkit.ChatColor;
import org.eytril.spigot.KeKSpigot;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CraftKnockbackProfile implements KnockbackProfile {

    private String name;
    private double friction = 2.0D;
    private double horizontal = 0.35D;
    private double vertical = 0.35D;
    private double verticalLimit = 0.4D;
    private double extraHorizontal = 0.425D;
    private double extraVertical = 0.085D;

    public CraftKnockbackProfile(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String[] getValues() {
        return new String[]{
                "Friction " + ChatColor.WHITE + ":" + this.friction,
                "Horizontal " + ChatColor.WHITE + ":" + this.horizontal,
                "Vertical " + ChatColor.WHITE + ":" + this.vertical,
                "Vertical Limit " + ChatColor.WHITE + ":" + this.verticalLimit,
                "Extra Horizontal " + ChatColor.WHITE + ":" + this.extraHorizontal,
                "Extra Vertical " + ChatColor.WHITE + ":" + this.extraVertical,
        };
    }

    public void save() {
        final String path = "knockback.profiles." + this.name;

        KeKSpigot.INSTANCE.getConfig().set(path + ".friction", this.friction);
        KeKSpigot.INSTANCE.getConfig().set(path + ".horizontal", this.horizontal);
        KeKSpigot.INSTANCE.getConfig().set(path + ".vertical", this.vertical);
        KeKSpigot.INSTANCE.getConfig().set(path + ".vertical-limit", this.verticalLimit);
        KeKSpigot.INSTANCE.getConfig().set(path + ".extra-horizontal", this.extraHorizontal);
        KeKSpigot.INSTANCE.getConfig().set(path + ".extra-vertical", this.extraVertical);
        KeKSpigot.INSTANCE.getConfig().save();
    }

}
