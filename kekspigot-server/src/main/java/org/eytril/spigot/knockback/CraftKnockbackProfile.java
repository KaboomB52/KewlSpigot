package org.eytril.spigot.knockback;

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

        KeKSpigot.INSTANCE.getConfig().set(path + ".friction", this.friction);
        KeKSpigot.INSTANCE.getConfig().set(path + ".horizontal", this.horizontal);
        KeKSpigot.INSTANCE.getConfig().set(path + ".vertical", this.vertical);
        KeKSpigot.INSTANCE.getConfig().set(path + ".vertical-limit", this.verticalLimit);
        KeKSpigot.INSTANCE.getConfig().set(path + ".extra-horizontal", this.extraHorizontal);
        KeKSpigot.INSTANCE.getConfig().set(path + ".extra-vertical", this.extraVertical);
        KeKSpigot.INSTANCE.getConfig().save();
    }

}
