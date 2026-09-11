package dev.krcboost;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;

@EventBusSubscriber(modid = KrcBoost.ID)
public final class BoostEvents {
    @SubscribeEvent public static void starting(ServerAboutToStartEvent event) { KrcBoost.reload(); }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) { BoostEngine.update(event.getEntity(), KrcBoost.config()); }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { BoostEngine.clear(event.getEntity()); }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) { BoostEngine.clear(event.getEntity()); }

    @SubscribeEvent public static void attack(AttackEntityEvent event) {
        // Revalidate equipment even if it changed between ticks.
        BoostEngine.update(event.getEntity(), KrcBoost.config());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void defense(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player) BoostEngine.update(player, KrcBoost.config());
    }

    @SubscribeEvent public static void damage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()
                || !(event.getSource().getEntity() instanceof Player player) || player == event.getEntity()) return;
        // One multiplier for player-attributed melee, projectiles, and KRC abilities.
        // It runs before armor/resistance and is not also applied to the attack attribute.
        if (KrcBoost.config().enabled() && BoostEngine.transformed(player)) {
            double amount = event.getAmount() * BoostEngine.damageMultiplier(player, KrcBoost.config());
            if (!Double.isFinite(amount) || amount > Float.MAX_VALUE)
                throw new IllegalArgumentException("Rider Power damage exceeds the game's finite float range");
            event.setAmount((float)amount);
        }
    }
}
