package dev.krcboost.gametest;

import dev.krcboost.*;
import com.kelco.kamenridercraft.item.base_items.RiderDriverItem;
import com.kelco.kamenridercraft.world.damagesource.RiderDamageTypes;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@Mod("krcboost_tests")
@GameTestHolder("krcboost")
@PrefixGameTestTemplate(false)
public final class IntegrationTests {
    private static final BoostConfig CONFIG = BoostConfig.defaults();

    static RiderDriverItem kuuga() {
        return (RiderDriverItem)BuiltInRegistries.ITEM.get(ResourceLocation.parse("kamenridercraft:arcle"));
    }
    static void equip(Player player, RiderDriverItem driver) {
        var stack = new ItemStack(driver);
        RiderDriverItem.resetFormItem(stack);
        player.setItemSlot(EquipmentSlot.FEET, stack);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(driver.helmet));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(driver.chestplate));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(driver.leggings));
        player.getAttribute(com.kelco.kamenridercraft.world.attribute.Attributes.IS_TRANSFORMING).setBaseValue(0);
    }
    static void near(GameTestHelper helper, double actual, double expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < 0.0001, message + ": expected " + expected + ", actual " + actual);
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void allRegisteredDriversUseTheSameBuff(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        int drivers = 0, active = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof RiderDriverItem driver)) continue;
            drivers++;
            BoostEngine.clear(player);
            equip(player, driver);
            boolean expected = driver.isTransformed(player);
            helper.assertTrue(BoostEngine.update(player, CONFIG) == expected, "Transformation mismatch: " + item);
            if (expected) {
                active++;
                helper.assertTrue(player.hasEffect(KrcBoost.RIDER_POWER), "Missing shared buff: " + item);
                near(helper, player.getMaxHealth(), 30, "Shared health multiplier");
            }
        }
        helper.assertTrue(drivers > 100 && active > 100, "Expected the actual KRC driver registry");
        KrcBoost.LOGGER.info("Rider Power coverage: {} actual KRC drivers, {} active complete base suits", drivers, active);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void repeatedTicksAndTransformationsCannotStackOrHeal(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setHealth(8);
        equip(player, kuuga());
        for (int i = 0; i < 200; i++) BoostEngine.update(player, CONFIG);
        near(helper, player.getMaxHealth(), 30, "No stacking maximum health");
        near(helper, player.getHealth(), 12, "Wounded player keeps the same fraction");
        for (int i = 0; i < 100; i++) {
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            BoostEngine.update(player, CONFIG);
            near(helper, player.getHealth(), 8, "Dehenshin cannot heal");
            equip(player, kuuga());
            BoostEngine.update(player, CONFIG);
        }
        near(helper, player.getHealth(), 12, "Repeated henshin cannot heal");
        helper.assertTrue(player.getAttribute(Attributes.MAX_HEALTH).getModifiers().stream()
                .filter(m -> m.id().equals(BoostEngine.MODIFIER)).count() == 1, "Exactly one modifier");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void fakeEffectIncompleteSuitAndAnimationGrantNothing(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.addEffect(new MobEffectInstance(KrcBoost.RIDER_POWER, -1, 255));
        BoostEngine.update(player, CONFIG);
        helper.assertTrue(!player.hasEffect(KrcBoost.RIDER_POWER), "Untransformed effect injection removed");
        near(helper, player.getMaxHealth(), 20, "No unauthorized health");
        equip(player, kuuga());
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        helper.assertTrue(!BoostEngine.update(player, CONFIG), "Incomplete suit rejected");
        equip(player, kuuga());
        player.getAttribute(com.kelco.kamenridercraft.world.attribute.Attributes.IS_TRANSFORMING).setBaseValue(20);
        helper.assertTrue(!BoostEngine.update(player, CONFIG), "Transformation animation is not completed");
        player.getAttribute(com.kelco.kamenridercraft.world.attribute.Attributes.IS_TRANSFORMING).setBaseValue(0);
        player.addEffect(new MobEffectInstance(KrcBoost.RIDER_POWER, 1000, 255));
        BoostEngine.update(player, CONFIG);
        helper.assertTrue(player.getEffect(KrcBoost.RIDER_POWER).getAmplifier() == 0, "Amplifier injection normalized");
        near(helper, player.getMaxHealth(), 30, "Amplifier cannot multiply server settings");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void removalBeforeAttackAndOtherModifiersArePreserved(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var unrelated = ResourceLocation.parse("test:other_mod");
        player.getAttribute(Attributes.ATTACK_DAMAGE).addTransientModifier(
                new AttributeModifier(unrelated, 5, AttributeModifier.Operation.ADD_VALUE));
        double original = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        equip(player, kuuga());
        BoostEngine.update(player, CONFIG);
        near(helper, player.getAttributeValue(Attributes.ATTACK_DAMAGE), original, "Damage multiplier does not double-apply through attributes");
        player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        NeoForge.EVENT_BUS.post(new AttackEntityEvent(player, player));
        near(helper, player.getAttributeValue(Attributes.ATTACK_DAMAGE), original, "Stale buff removed before attack");
        helper.assertTrue(player.getAttribute(Attributes.ATTACK_DAMAGE).hasModifier(unrelated), "Other mod preserved");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void logoutSaveReloadAndDeathDoNotCreateHealth(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setHealth(8);
        equip(player, kuuga());
        BoostEngine.update(player, CONFIG);
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(player));
        near(helper, player.getHealth(), 8, "Logout normalizes before vanilla saves");
        CompoundTag saved = player.saveWithoutId(new CompoundTag());
        var restored = helper.makeMockPlayer(GameType.SURVIVAL);
        restored.load(saved);
        BoostEngine.update(restored, CONFIG);
        near(helper, restored.getHealth() / restored.getMaxHealth(), 0.4, "Reconnect health ratio");
        restored.setHealth(0);
        BoostEngine.update(restored, CONFIG);
        near(helper, restored.getHealth(), 0, "No revival on death");
        helper.assertTrue(!restored.hasEffect(KrcBoost.RIDER_POWER), "Dead player buff cleared");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void serverRejectsUnauthorizedConfigAndStaleRevisions(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        var ordinary = new ServerPlayer(server, helper.getLevel(), new GameProfile(UUID.randomUUID(), "ordinary"),
                ClientInformation.createDefault()) {
            @Override protected int getPermissionLevel() { return 0; }
        };
        var admin = new ServerPlayer(server, helper.getLevel(), new GameProfile(UUID.randomUUID(), "administrator"),
                ClientInformation.createDefault()) {
            @Override protected int getPermissionLevel() { return 2; }
        };
        BoostConfig old = KrcBoost.config();
        long revision = KrcBoost.revision();
        boolean rejected = false;
        try { EditorProtocol.apply(ordinary, new BoostConfig(true, 999, 999, 0, 0, 1, 0), revision); }
        catch (IllegalArgumentException expected) { rejected = true; }
        helper.assertTrue(rejected && KrcBoost.config().equals(old), "Ordinary players cannot save forged settings");
        var editCommand = server.getCommands().getDispatcher().getRoot().getChild("krcboost").getChild("edit");
        helper.assertTrue(!editCommand.canUse(ordinary.createCommandSourceStack()), "Command permission enforced");
        try {
            EditorProtocol.apply(admin, old, revision);
            rejected = false;
            try { EditorProtocol.apply(admin, old, revision); }
            catch (IllegalArgumentException expected) { rejected = true; }
            helper.assertTrue(rejected, "Stale editor cannot overwrite new server settings");
        } finally {
            KrcBoost.save(old, KrcBoost.revision());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void disabledConfigClearsActiveBuff(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        equip(player, kuuga());
        BoostEngine.update(player, CONFIG);
        var disabled = new BoostConfig(false, 1.5, 1.5, 4, 2, 1.15, 0.1);
        BoostEngine.update(player, disabled);
        helper.assertTrue(!player.hasEffect(KrcBoost.RIDER_POWER), "Disabled means no effect");
        near(helper, player.getMaxHealth(), 20, "Disabled health restored");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void attacksAndRiderKicksBoostOnceOnlyWhenTransformed(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var victim = helper.makeMockPlayer(GameType.SURVIVAL);
        equip(player, kuuga());
        var kick = new DamageSource(helper.getLevel().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(RiderDamageTypes.RIDER_KICK), player, player);
        var arrow = new net.minecraft.world.entity.projectile.Arrow(net.minecraft.world.entity.EntityType.ARROW, helper.getLevel());
        for (var source : new DamageSource[]{player.damageSources().playerAttack(player), kick, player.damageSources().arrow(arrow, player)}) {
            var event = new LivingIncomingDamageEvent(victim, new DamageContainer(source, 10));
            NeoForge.EVENT_BUS.post(event);
            near(helper, event.getAmount(), 15, "One shared damage multiplier");
        }
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        var event = new LivingIncomingDamageEvent(victim, new DamageContainer(kick, 10));
        NeoForge.EVENT_BUS.post(event);
        near(helper, event.getAmount(), 10, "No stale damage buff after equipment removal");
        helper.succeed();
    }
}
