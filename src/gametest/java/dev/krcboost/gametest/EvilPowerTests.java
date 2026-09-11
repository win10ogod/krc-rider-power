package dev.krcboost.gametest;

import com.google.gson.JsonParser;
import com.kelco.kamenridercraft.world.damagesource.RiderDamageTypes;
import dev.krcboost.*;
import java.nio.file.Files;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("krcboost")
@PrefixGameTestTemplate(false)
public final class EvilPowerTests {
    private static final BoostConfig BASE = BoostConfig.defaults();
    private static void near(GameTestHelper helper, double actual, double expected, String message) {
        IntegrationTests.near(helper, actual, expected, message);
    }
    private static void score(GameTestHelper helper, Player player, int target) {
        // Test fixture only: allow repeated transitions without changing production reward rules.
        var rules = new KarmaSettings(true, 100, 10, 1, 20, 1_000_000);
        KarmaSavedData.get(helper.getLevel().getServer()).change(player.getUUID(), target - KarmaSystem.score(player),
                helper.getLevel().getServer().overworld().getGameTime(), rules);
    }
    private static void onlyEvil(GameTestHelper helper, Player player) {
        helper.assertTrue(player.hasEffect(KrcBoost.EVIL_RIDER_POWER) && !player.hasEffect(KrcBoost.RIDER_POWER), "Only evil power active");
        helper.assertTrue(player.getEffect(KrcBoost.EVIL_RIDER_POWER).getAmplifier() == 0, "Evil power stays level I");
    }
    private static void neither(GameTestHelper helper, Player player) {
        helper.assertTrue(!player.hasEffect(KrcBoost.EVIL_RIDER_POWER) && !player.hasEffect(KrcBoost.RIDER_POWER), "Both powers removed");
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void switchingAndInjectedEffectsCannotStackOrHeal(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setHealth(8);
        IntegrationTests.equip(player, IntegrationTests.kuuga());
        player.addEffect(new MobEffectInstance(KrcBoost.EVIL_RIDER_POWER, -1, 255));
        BoostEngine.update(player, BASE);
        helper.assertTrue(player.hasEffect(KrcBoost.RIDER_POWER) && !player.hasEffect(KrcBoost.EVIL_RIDER_POWER), "Neutral removes injected evil effect");
        for (int i = 0; i < 100; i++) {
            score(helper, player, -100);
            player.addEffect(new MobEffectInstance(KrcBoost.RIDER_POWER, -1, 255));
            player.addEffect(new MobEffectInstance(KrcBoost.EVIL_RIDER_POWER, 200, 255));
            BoostEngine.update(player, BASE);
            onlyEvil(helper, player);
            near(helper, player.getMaxHealth(), 10, "Evil health does not stack");
            near(helper, player.getHealth(), 4, "Switching to evil preserves wounds");
            score(helper, player, 100);
            BoostEngine.update(player, BASE);
            helper.assertTrue(player.hasEffect(KrcBoost.RIDER_POWER) && !player.hasEffect(KrcBoost.EVIL_RIDER_POWER), "Good removes evil effect");
            near(helper, player.getMaxHealth(), 40, "Good health restored");
            near(helper, player.getHealth(), 16, "Switching back cannot refill health");
        }
        score(helper, player, -100);
        BoostEngine.update(player, BASE);
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        BoostEngine.update(player, BASE);
        neither(helper, player);
        near(helper, player.getHealth(), 8, "Leaving the transformation preserves the original ratio");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void evilMeleeProjectileAndRiderKickDamageApplyOnce(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var victim = helper.makeMockPlayer(GameType.SURVIVAL);
        IntegrationTests.equip(player, IntegrationTests.kuuga());
        score(helper, player, -100);
        var kick = new DamageSource(helper.getLevel().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(RiderDamageTypes.RIDER_KICK), player, player);
        var arrow = EntityType.ARROW.create(helper.getLevel());
        for (var source : new DamageSource[]{player.damageSources().playerAttack(player), kick, player.damageSources().arrow(arrow, player)}) {
            var event = new LivingIncomingDamageEvent(victim, new DamageContainer(source, 10));
            NeoForge.EVENT_BUS.post(event);
            near(helper, event.getAmount(), 25, "Evil damage is multiplied once using server karma");
        }
        var self = new LivingIncomingDamageEvent(player, new DamageContainer(kick, 10));
        NeoForge.EVENT_BUS.post(self);
        near(helper, self.getAmount(), 10, "Evil power does not amplify self damage");
        player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        var stale = new LivingIncomingDamageEvent(victim, new DamageContainer(kick, 10));
        NeoForge.EVENT_BUS.post(stale);
        near(helper, stale.getAmount(), 10, "Removed driver cannot retain evil damage");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void evilLogoutReloadDeathAndDisablePreserveHealth(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setHealth(8);
        IntegrationTests.equip(player, IntegrationTests.kuuga());
        score(helper, player, -100);
        BoostEngine.update(player, BASE);
        onlyEvil(helper, player);
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(player));
        neither(helper, player);
        near(helper, player.getHealth(), 8, "Evil logout restores underlying wounded health before save");
        // Vanilla restores UUID from GameProfile during load; reconnect must use the same account.
        var restored = new Player(helper.getLevel(), BlockPos.ZERO, 0, player.getGameProfile()) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return false; }
            @Override public boolean isLocalPlayer() { return true; }
        };
        restored.load(player.saveWithoutId(new CompoundTag()));
        near(helper, KarmaSystem.score(restored), -100, "Reconnect keeps the same player's saved karma");
        helper.assertTrue(BoostEngine.transformed(restored), "Reconnect restores the complete KRC transformation");
        BoostEngine.update(restored, BASE);
        onlyEvil(helper, restored);
        near(helper, restored.getHealth(), 4, "Evil reconnect cannot heal");
        BoostEngine.update(restored, new BoostConfig(false, 1.5, 1.5, 4, 2, 1.15, 0.1));
        neither(helper, restored);
        near(helper, restored.getMaxHealth(), 20, "Disabling removes the health reduction");
        near(helper, restored.getHealth(), 8, "Disabling preserves wounds");
        BoostEngine.update(restored, BASE);
        restored.setHealth(0);
        BoostEngine.update(restored, BASE);
        neither(helper, restored);
        near(helper, restored.getHealth(), 0, "Evil death cannot revive the player");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "karma_configuration", timeoutTicks = 100)
    public static void reloadingDisabledOrCustomKarmaUpdatesTheSelectedPower(GameTestHelper helper) throws Exception {
        var path = KrcBoost.karmaConfigPath();
        String original = Files.readString(path);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setHealth(8);
        IntegrationTests.equip(player, IntegrationTests.kuuga());
        score(helper, player, -100);
        try {
            BoostEngine.update(player, BASE);
            onlyEvil(helper, player);
            var json = JsonParser.parseString(original).getAsJsonObject();
            json.addProperty("enabled", false);
            Files.writeString(path, json.toString());
            KrcBoost.reload();
            BoostEngine.update(player, BASE);
            helper.assertTrue(player.hasEffect(KrcBoost.RIDER_POWER) && !player.hasEffect(KrcBoost.EVIL_RIDER_POWER), "Disabling karma selects normal base power");
            near(helper, player.getHealth(), 12, "Karma disable preserves wounds at base maximum health");
            near(helper, BoostEngine.damageMultiplier(player, BASE), 1.5, "Karma disable restores base damage");
            var zombie = EntityType.ZOMBIE.create(helper.getLevel());
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(zombie, player.damageSources().playerAttack(player)));
            near(helper, KarmaSystem.score(player), -100, "Disabled rules retain the saved history");
            json.addProperty("enabled", true);
            json.addProperty("evilDamageBonusScale", 4);
            json.addProperty("evilHealthMultiplier", 0.25);
            Files.writeString(path, json.toString());
            KrcBoost.reload();
            BoostEngine.update(player, BASE);
            onlyEvil(helper, player);
            near(helper, player.getMaxHealth(), 5, "Reload applies the custom health endpoint");
            near(helper, player.getHealth(), 2, "Custom endpoint preserves the wounded ratio");
            near(helper, BoostEngine.damageMultiplier(player, BASE), 3, "Reload applies custom damage scaling");
        } finally {
            Files.writeString(path, original);
            KrcBoost.reload();
            BoostEngine.clear(player);
        }
        helper.succeed();
    }
}
