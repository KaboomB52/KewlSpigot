package org.eytril.spigot.knockback;

import org.bukkit.ChatColor;
import org.eytril.spigot.KewlSpigot;
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
                ChatColor.AQUA + "Friction" + ChatColor.WHITE + ": " + this.friction,
                ChatColor.AQUA + "Horizontal" + ChatColor.WHITE + ": " + this.horizontal,
                ChatColor.AQUA + "Vertical" + ChatColor.WHITE + ": " + this.vertical,
                ChatColor.AQUA + "Vertical Limit" + ChatColor.WHITE + ": " + this.verticalLimit,
                ChatColor.AQUA + "Extra Horizontal" + ChatColor.WHITE + ": " + this.extraHorizontal,
                ChatColor.AQUA + "Extra Vertical" + ChatColor.WHITE + ": " + this.extraVertical,
        };
    }

    public void save() {
        final String path = "knockback.profiles." + this.name;

        KewlSpigot.INSTANCE.getConfig().set(path + ".friction", this.friction);
        KewlSpigot.INSTANCE.getConfig().set(path + ".horizontal", this.horizontal);
        KewlSpigot.INSTANCE.getConfig().set(path + ".vertical", this.vertical);
        KewlSpigot.INSTANCE.getConfig().set(path + ".vertical-limit", this.verticalLimit);
        KewlSpigot.INSTANCE.getConfig().set(path + ".extra-horizontal", this.extraHorizontal);
        KewlSpigot.INSTANCE.getConfig().set(path + ".extra-vertical", this.extraVertical);
        KewlSpigot.INSTANCE.getConfig().save();
    }

}
