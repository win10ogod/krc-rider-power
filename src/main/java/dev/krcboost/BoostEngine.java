package dev.krcboost;

import com.kelco.kamenridercraft.item.base_items.RiderDriverItem;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import java.util.List;

/** A single server-validated buff. Stable IDs prevent repeated ticks from stacking it. */
public final class BoostEngine {
    public static final ResourceLocation MODIFIER = ResourceLocation.fromNamespaceAndPath(KrcBoost.ID, "rider_power");
    private static final List<Holder<Attribute>> ATTRIBUTES = List.of(Attributes.MAX_HEALTH,
            Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS, Attributes.MOVEMENT_SPEED, Attributes.KNOCKBACK_RESISTANCE);
    public static boolean transformed(Player player) {
        return player.isAlive() && !player.isSpectator()
                && player.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof RiderDriverItem driver
                && driver.isTransformed(player) && !RiderDriverItem.isTransforming(player);
    }
    public static boolean update(Player player, BoostConfig config) {
        if (player.level().isClientSide()) return false;
        if (!config.enabled() || !transformed(player)) {
            clear(player);
            return false;
        }
        var effect = player.getEffect(KrcBoost.RIDER_POWER);
        if (effect == null || effect.getAmplifier() != 0 || !effect.isInfiniteDuration()) {
            if (effect != null) player.removeEffect(KrcBoost.RIDER_POWER);
            player.addEffect(new MobEffectInstance(KrcBoost.RIDER_POWER, -1, 0, false, false, true));
        }
        double oldMax = player.getMaxHealth();
        float health = player.getHealth();
        double strength = KarmaSystem.strength(player);
        multiplier(player, Attributes.MAX_HEALTH, 1 + (config.healthMultiplier() - 1) * strength);
        bonus(player, Attributes.ARMOR, config.armorBonus() * strength);
        bonus(player, Attributes.ARMOR_TOUGHNESS, config.toughnessBonus() * strength);
        multiplier(player, Attributes.MOVEMENT_SPEED, 1 + (config.speedMultiplier() - 1) * strength);
        bonus(player, Attributes.KNOCKBACK_RESISTANCE, config.knockbackBonus() * strength);
        restoreHealthFraction(player, health, oldMax);
        return true;
    }
    public static double damageMultiplier(Player player, BoostConfig config) {
        return 1 + (config.attackMultiplier() - 1) * KarmaSystem.strength(player);
    }
    private static void multiplier(Player player, Holder<Attribute> attr, double factor) {
        put(player, attr, factor - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
    private static void bonus(Player player, Holder<Attribute> attr, double amount) {
        put(player, attr, amount, AttributeModifier.Operation.ADD_VALUE);
    }
    private static void put(Player player, Holder<Attribute> attr, double amount, AttributeModifier.Operation operation) {
        var instance = player.getAttribute(attr);
        if (instance == null) return;
        var previous = instance.getModifier(MODIFIER);
        if (amount == 0) {
            if (previous != null) instance.removeModifier(MODIFIER);
        } else if (previous == null || previous.amount() != amount || previous.operation() != operation) {
            if (previous != null) instance.removeModifier(MODIFIER);
            instance.addTransientModifier(new AttributeModifier(MODIFIER, amount, operation));
        }
    }
    public static void clear(Player player) {
        if (player.level().isClientSide()) return;
        double oldMax = player.getMaxHealth();
        float health = player.getHealth();
        player.removeEffect(KrcBoost.RIDER_POWER);
        for (var attr : ATTRIBUTES) {
            var instance = player.getAttribute(attr);
            if (instance != null && instance.hasModifier(MODIFIER)) instance.removeModifier(MODIFIER);
        }
        restoreHealthFraction(player, health, oldMax);
    }
    private static void restoreHealthFraction(Player player, float health, double oldMax) {
        double nextMax = player.getMaxHealth();
        if (player.isAlive() && oldMax > 0 && oldMax != nextMax)
            player.setHealth((float)Math.min(nextMax, health / oldMax * nextMax));
    }
}
