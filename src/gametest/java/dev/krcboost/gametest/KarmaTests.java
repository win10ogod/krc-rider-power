package dev.krcboost.gametest;

import dev.krcboost.*;
import java.util.UUID;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobSplitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("krcboost")
@PrefixGameTestTemplate(false)
public final class KarmaTests {
    private static void near(GameTestHelper helper, double actual, double expected, String message) {
        IntegrationTests.near(helper, actual, expected, message);
    }
    private static void origin(Mob mob, MobSpawnType type) {
        var data = mob.saveWithoutId(new CompoundTag());
        data.putString("neoforge:spawn_type", type.name());
        mob.load(data);
        mob.setNoAi(true);
    }
    private static void starter(ZombieVillager zombie, UUID player, int ticks) {
        var data = zombie.saveWithoutId(new CompoundTag());
        data.putUUID("ConversionPlayer", player);
        data.putInt("ConversionTime", ticks);
        zombie.load(data);
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void animalsAndPlayersNeverAffectKarma(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        var wolf = new Wolf(EntityType.WOLF, helper.getLevel());
        wolf.setTame(true, true);
        wolf.setOwnerUUID(player.getUUID());
        var horse = new Horse(EntityType.HORSE, helper.getLevel());
        horse.setTamed(true);
        for (var target : new net.minecraft.world.entity.LivingEntity[]{wolf, horse,
                EntityType.COW.create(helper.getLevel()), helper.makeMockPlayer(GameType.SURVIVAL)}) {
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(target, player.damageSources().playerAttack(player)));
            near(helper, KarmaSystem.score(player), 0, "Animals, tamed pets, and PVP are neutral");
        }
        var villager = helper.spawn(EntityType.VILLAGER, 0, 2, 0);
        villager.setNoAi(true);
        villager.hurt(player.damageSources().playerAttack(player), 1000);
        near(helper, KarmaSystem.score(player), -20, "Actual villager kill is penalized while untransformed");
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(villager, player.damageSources().playerAttack(player)));
        near(helper, KarmaSystem.score(player), -20, "One penalty per victim");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void hostilePlayerAndProjectileKillsEarnBoundedRewards(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        var zombie = helper.spawn(EntityType.ZOMBIE, 0, 2, 0);
        origin(zombie, MobSpawnType.NATURAL);
        var arrow = EntityType.ARROW.create(helper.getLevel());
        var source = player.damageSources().arrow(arrow, player);
        zombie.hurt(source, 1000);
        near(helper, KarmaSystem.score(player), 1, "Actual player-attributed projectile kill earns one point");
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(zombie, source));
        near(helper, KarmaSystem.score(player), 1, "Duplicate death cannot grant extra points");
        var environmental = EntityType.ZOMBIE.create(helper.getLevel());
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(environmental, environmental.damageSources().generic()));
        near(helper, KarmaSystem.score(player), 1, "Unattributed deaths grant no reward");
        for (int i = 0; i < 100; i++) {
            var target = EntityType.SKELETON.create(helper.getLevel());
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(target, player.damageSources().playerAttack(player)));
        }
        near(helper, KarmaSystem.score(player), 30, "Hostile kills share the positive reward budget");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void artificialSpawnAndConversionCannotLaunderRewards(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (var type : new MobSpawnType[]{MobSpawnType.SPAWNER, MobSpawnType.SPAWN_EGG,
                MobSpawnType.COMMAND, MobSpawnType.DISPENSER, MobSpawnType.MOB_SUMMONED}) {
            var zombie = EntityType.ZOMBIE.create(helper.getLevel());
            origin(zombie, type);
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(zombie, player.damageSources().playerAttack(player)));
            var drowned = EntityType.DROWNED.create(helper.getLevel());
            NeoForge.EVENT_BUS.post(new LivingConversionEvent.Post(zombie, drowned));
            var saved = drowned.saveWithoutId(new CompoundTag());
            var restored = EntityType.DROWNED.create(helper.getLevel());
            restored.load(saved);
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(restored, player.damageSources().playerAttack(player)));
        }
        near(helper, KarmaSystem.score(player), 0, "Artificial spawn exclusions survive conversion and NBT reload");
        var slime = EntityType.SLIME.create(helper.getLevel());
        origin(slime, MobSpawnType.SPAWNER);
        var child = EntityType.SLIME.create(helper.getLevel());
        NeoForge.EVENT_BUS.post(new MobSplitEvent(slime, List.of(child)));
        var restoredChild = EntityType.SLIME.create(helper.getLevel());
        restoredChild.load(child.saveWithoutId(new CompoundTag()));
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(restoredChild, player.damageSources().playerAttack(player)));
        near(helper, KarmaSystem.score(player), 0, "Spawner slimes cannot earn points by splitting, even after reload");
        var trial = EntityType.ZOMBIE.create(helper.getLevel());
        origin(trial, MobSpawnType.TRIAL_SPAWNER);
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(trial, player.damageSources().playerAttack(player)));
        near(helper, KarmaSystem.score(player), 1, "Trial chamber combat remains eligible");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void actualCompletedCureCreditsTheCorrectOfflinePlayer(GameTestHelper helper) {
        UUID rescuer = UUID.randomUUID();
        var zombie = helper.spawn(EntityType.ZOMBIE_VILLAGER, 0, 2, 0);
        origin(zombie, MobSpawnType.NATURAL);
        starter(zombie, rescuer, 1);
        helper.succeedWhen(() -> {
            near(helper, KarmaSavedData.get(helper.getLevel().getServer()).progress(rescuer).score(), 10,
                    "A completed vanilla cure credits its saved ConversionPlayer even when offline");
            helper.assertTrue(zombie.isRemoved(), "Conversion really completed");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void repeatedAndManufacturedCuresDoNotEarnPoints(GameTestHelper helper) {
        UUID rescuer = UUID.randomUUID();
        var zombie = EntityType.ZOMBIE_VILLAGER.create(helper.getLevel());
        starter(zombie, rescuer, 100);
        var villager = EntityType.VILLAGER.create(helper.getLevel());
        NeoForge.EVENT_BUS.post(new LivingConversionEvent.Post(zombie, villager));
        NeoForge.EVENT_BUS.post(new LivingConversionEvent.Post(zombie, villager));
        var infected = EntityType.ZOMBIE_VILLAGER.create(helper.getLevel());
        NeoForge.EVENT_BUS.post(new LivingConversionEvent.Post(villager, infected));
        starter(infected, rescuer, 100);
        var restored = EntityType.ZOMBIE_VILLAGER.create(helper.getLevel());
        restored.load(infected.saveWithoutId(new CompoundTag()));
        NeoForge.EVENT_BUS.post(new LivingConversionEvent.Post(restored, EntityType.VILLAGER.create(helper.getLevel())));
        var firstInfection = EntityType.ZOMBIE_VILLAGER.create(helper.getLevel());
        NeoForge.EVENT_BUS.post(new LivingConversionEvent.Post(EntityType.VILLAGER.create(helper.getLevel()), firstInfection));
        starter(firstInfection, rescuer, 100);
        NeoForge.EVENT_BUS.post(new LivingConversionEvent.Post(firstInfection, EntityType.VILLAGER.create(helper.getLevel())));
        near(helper, KarmaSavedData.get(helper.getLevel().getServer()).progress(rescuer).score(), 10,
                "Only the first eligible rescue earns credit; infection cycles remain excluded");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void karmaScalesAllBonusesAndPreservesWoundedHealth(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var rules = new KarmaSettings(true, 100, 10, 1, 20, 10000);
        var store = KarmaSavedData.get(helper.getLevel().getServer());
        long now = helper.getLevel().getGameTime();
        player.setHealth(8);
        IntegrationTests.equip(player, IntegrationTests.kuuga());
        BoostEngine.update(player, BoostConfig.defaults());
        near(helper, player.getMaxHealth(), 30, "Neutral retains the original maximum health");
        near(helper, player.getHealth(), 12, "Neutral preserves health ratio");
        store.change(player.getUUID(), 100, now, rules);
        BoostEngine.update(player, BoostConfig.defaults());
        near(helper, player.getMaxHealth(), 40, "Maximum good doubles only the added health");
        near(helper, player.getHealth(), 16, "Good karma cannot refill health");
        near(helper, BoostEngine.damageMultiplier(player, BoostConfig.defaults()), 2, "Damage bonus scales with karma");
        near(helper, player.getAttribute(Attributes.ARMOR).getModifier(BoostEngine.MODIFIER).amount(), 8, "Armor bonus doubles");
        near(helper, player.getAttribute(Attributes.ARMOR_TOUGHNESS).getModifier(BoostEngine.MODIFIER).amount(), 4, "Toughness bonus doubles");
        near(helper, player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(BoostEngine.MODIFIER).amount(), 0.3, "Speed bonus doubles");
        near(helper, player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getModifier(BoostEngine.MODIFIER).amount(), 0.2, "Knockback bonus doubles");
        store.change(player.getUUID(), -200, now, rules);
        BoostEngine.update(player, BoostConfig.defaults());
        near(helper, player.getMaxHealth(), 20, "Maximum evil removes the addon health bonus");
        near(helper, player.getHealth(), 8, "Evil preserves health ratio");
        near(helper, BoostEngine.damageMultiplier(player, BoostConfig.defaults()), 1, "No extra damage at minimum karma");
        store.change(player.getUUID(), 200, now, rules);
        for (int i = 0; i < 100; i++) {
            BoostEngine.update(player, BoostConfig.defaults());
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            BoostEngine.update(player, BoostConfig.defaults());
            IntegrationTests.equip(player, IntegrationTests.kuuga());
        }
        near(helper, player.getHealth() / player.getMaxHealth(), 0.4, "Karma-aware transformation cycles cannot heal");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void karmaAndRewardBudgetSurviveSaveLoadDeathAndPlayerClone(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var store = KarmaSavedData.get(helper.getLevel().getServer());
        store.change(player.getUUID(), 30, helper.getLevel().getGameTime(), KarmaSettings.defaults());
        var restored = KarmaSavedData.load(store.save(new CompoundTag(), helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        near(helper, restored.progress(player.getUUID()).score(), 30, "Saved score survives reload");
        near(helper, restored.progress(player.getUUID()).rewardedInWindow(), 30, "Reward budget survives reload");
        near(helper, restored.change(player.getUUID(), 10, helper.getLevel().getGameTime(), KarmaSettings.defaults()), 0, "Reload does not restore reward budget");
        var clone = helper.makeMockPlayer(GameType.SURVIVAL);
        clone.setUUID(player.getUUID());
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(clone, player, true));
        near(helper, KarmaSystem.score(clone), 30, "Death and new player instances do not reset karma");
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(clone));
        near(helper, KarmaSystem.score(clone), 30, "Logout does not reset karma");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void cancelledDeathsAndForgedPlayerDataCannotChangeKarma(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getPersistentData().putInt("krcboost:karma", 100);
        var death = new LivingDeathEvent(EntityType.VILLAGER.create(helper.getLevel()), player.damageSources().playerAttack(player));
        death.setCanceled(true);
        KarmaSystem.killed(death);
        near(helper, KarmaSystem.score(player), 0, "Cancelled death and client-style player data grant no score");
        var command = helper.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("krcboost").getChild("karma");
        helper.assertTrue(command.getChildren().isEmpty(), "Karma command has no score-writing subcommands");
        helper.succeed();
    }
}
