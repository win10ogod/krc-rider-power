package dev.krcboost;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/** Renew a short vanilla effect without removing or downgrading effects from other sources. */
public final class HeroReward {
    public static final int REQUIRED_SCORE = 100;
    private static final int DURATION_TICKS = 200;

    public static void update(Player player) {
        if (player.level().isClientSide() || !player.isAlive() || player.isSpectator()
                || !KrcBoost.karmaSettings().enabled() || KarmaSystem.score(player) < REQUIRED_SCORE) return;
        var current = player.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
        if (current == null || (current.getAmplifier() == 0 && !current.isInfiniteDuration()
                && current.getDuration() <= DURATION_TICKS / 2)) {
            player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE,
                    DURATION_TICKS, 0, false, false, true));
        }
    }
}
