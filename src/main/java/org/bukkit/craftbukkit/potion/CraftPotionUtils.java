
package org.bukkit.craftbukkit.potion;

import net.minecraft.server.MobEffect;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CraftPotionUtils {
    private CraftPotionUtils() {}

    public static PotionEffect toBukkit(MobEffect effect) {
        return new PotionEffect(PotionEffectType.getById(effect.getEffectId()),
                effect.getDuration(),
                effect.getAmplifier(),
                effect.isAmbient(),
                effect.isShowParticles());
    }

    public static MobEffect toNMS(PotionEffect effect) {
        return new MobEffect(effect.getType().getId(),
                effect.getDuration(),
                effect.getAmplifier(),
                effect.isAmbient(),
                effect.hasParticles());
    }

    public static MobEffect cloneWithDuration(MobEffect effect, int duration) {
        return new MobEffect(effect.getEffectId(), duration, effect.getAmplifier(), effect.isAmbient(), effect.isShowParticles());
    }

    public static void extendDuration(MobEffect effect, int duration) {
        effect.a(cloneWithDuration(effect, duration));
    }
}
