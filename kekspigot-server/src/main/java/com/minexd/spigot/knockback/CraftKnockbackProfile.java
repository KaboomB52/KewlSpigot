package com.minexd.spigot.knockback;

import com.minexd.spigot.SpigotX;
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
                "Friction: " + this.friction,
                "Horizontal: " + this.horizontal,
                "Vertical: " + this.vertical,
                "Vertical Limit: " + this.verticalLimit,
                "Extra Horizontal: " + this.extraHorizontal,
                "Extra Vertical: " + this.extraVertical,
        };
    }

    public void save() {
        final String path = "knockback.profiles." + this.name;

        SpigotX.INSTANCE.getConfig().set(path + ".friction", this.friction);
        SpigotX.INSTANCE.getConfig().set(path + ".horizontal", this.horizontal);
        SpigotX.INSTANCE.getConfig().set(path + ".vertical", this.vertical);
        SpigotX.INSTANCE.getConfig().set(path + ".vertical-limit", this.verticalLimit);
        SpigotX.INSTANCE.getConfig().set(path + ".extra-horizontal", this.extraHorizontal);
        SpigotX.INSTANCE.getConfig().set(path + ".extra-vertical", this.extraVertical);
        SpigotX.INSTANCE.getConfig().save();
    }

}
