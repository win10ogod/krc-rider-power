package dev.krcboost;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobSplitEvent;
import net.neoforged.neoforge.common.util.FakePlayer;

@EventBusSubscriber(modid = KrcBoost.ID)
public final class KarmaSystem {
    public static final String CURE_USED = "krcboost:cure_reward_used";
    public static final String NO_REWARDS = "krcboost:no_karma_rewards";
    private static final String DEATH_COUNTED = "krcboost:death_counted";
    public static int score(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return 0;
        return KrcBoost.karmaSettings().bound(KarmaSavedData.get(level.getServer()).progress(player.getUUID()).score());
    }
    public static double strength(Player player) { return KrcBoost.karmaSettings().strength(score(player)); }
    public static boolean rewardEligible(Mob mob) {
        if (mob.getPersistentData().getBoolean(NO_REWARDS)) return false;
        MobSpawnType origin = mob.getSpawnType();
        return origin != MobSpawnType.SPAWNER && origin != MobSpawnType.SPAWN_EGG
                && origin != MobSpawnType.COMMAND && origin != MobSpawnType.DISPENSER
                && origin != MobSpawnType.MOB_SUMMONED && origin != MobSpawnType.BREEDING;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void split(MobSplitEvent event) {
        // Slime children do not inherit spawn type, so retain their parent's exclusion explicitly.
        if (!rewardEligible(event.getParent()))
            for (Mob child : event.getChildren()) child.getPersistentData().putBoolean(NO_REWARDS, true);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void killed(LivingDeathEvent event) {
        if (!KrcBoost.karmaSettings().enabled() || event.isCanceled()
                || event.getEntity().level().isClientSide()) return;
        var victim = event.getEntity();
        if (victim instanceof Animal || victim instanceof Player) return;
        boolean villager = victim instanceof AbstractVillager;
        boolean hostile = victim instanceof Enemy && victim instanceof Mob mob && rewardEligible(mob);
        if (!villager && !hostile) return;
        Entity responsible = event.getSource().getEntity();
        if (responsible == null) responsible = event.getEntity().getKillCredit();
        if (!(responsible instanceof Player player) || player instanceof FakePlayer) return;
        var playerId = player.getUUID();
        var data = event.getEntity().getPersistentData();
        if (data.getBoolean(DEATH_COUNTED)) return;
        data.putBoolean(DEATH_COUNTED, true);
        var level = (ServerLevel)event.getEntity().level();
        int requested = villager ? -KrcBoost.karmaSettings().villagerKillPenalty() : KrcBoost.karmaSettings().hostileKillReward();
        int delta = KarmaSavedData.get(level.getServer()).change(playerId, requested,
                level.getServer().overworld().getGameTime(), KrcBoost.karmaSettings());
        changed(player, delta, villager ? "krcboost.karma.villager_kill" : "krcboost.karma.hostile_kill");
    }
    @SubscribeEvent
    public static void converted(LivingConversionEvent.Post event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        LivingEntity previous = event.getEntity(), next = event.getOutcome();
        if (previous instanceof Mob mob && !rewardEligible(mob) || previous instanceof Villager)
            next.getPersistentData().putBoolean(NO_REWARDS, true);
        // An infected villager is not a new rescue target. Preserve this across conversions/reloads.
        if (previous instanceof Villager || previous.getPersistentData().getBoolean(CURE_USED))
            next.getPersistentData().putBoolean(CURE_USED, true);
        if (!(previous instanceof ZombieVillager zombie) || !(next instanceof Villager)
                || previous.getPersistentData().getBoolean(CURE_USED) || !rewardEligible(zombie)) return;
        previous.getPersistentData().putBoolean(CURE_USED, true);
        next.getPersistentData().putBoolean(CURE_USED, true);
        if (!KrcBoost.karmaSettings().enabled()) return;
        // Vanilla records the player who started the successful cure in this saved field.
        CompoundTag saved = zombie.saveWithoutId(new CompoundTag());
        if (!saved.hasUUID("ConversionPlayer")) return;
        var id = saved.getUUID("ConversionPlayer");
        int delta = KarmaSavedData.get(level.getServer()).change(id, KrcBoost.karmaSettings().cureReward(),
                level.getServer().overworld().getGameTime(), KrcBoost.karmaSettings());
        var player = level.getServer().getPlayerList().getPlayer(id);
        if (player != null) changed(player, delta, "krcboost.karma.cure");
    }
    private static void changed(Player player, int delta, String reason) {
        if (delta == 0) return;
        BoostEngine.update(player, KrcBoost.config());
        if (player instanceof ServerPlayer)
            player.displayClientMessage(Component.translatable("krcboost.karma.changed", Component.translatable(reason),
                    (delta > 0 ? "+" : "") + delta, score(player), Math.round(strength(player) * 100)), true);
    }
}
