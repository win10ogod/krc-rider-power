package dev.krcboost;

import com.mojang.logging.LogUtils;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import org.slf4j.Logger;
import java.nio.file.Path;

@Mod(KrcBoost.ID)
public final class KrcBoost {
    public static final String ID = "krcboost";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ID);
    public static final DeferredHolder<MobEffect, RiderPowerEffect> RIDER_POWER = EFFECTS.register("rider_power", RiderPowerEffect::new);
    private static volatile BoostConfig config = BoostConfig.defaults();
    private static long revision;
    public static final class RiderPowerEffect extends MobEffect {
        private RiderPowerEffect() { super(MobEffectCategory.BENEFICIAL, 0x43CBB1); }
    }
    public KrcBoost(IEventBus modBus) {
        EFFECTS.register(modBus);
        modBus.addListener(EditorProtocol::register);
    }
    public static Path configPath() { return FMLPaths.CONFIGDIR.get().resolve("krcboost.json"); }
    public static BoostConfig config() { return config; }
    public static long revision() { return revision; }
    public static void reload() {
        try {
            BoostConfig next = BoostConfig.load(configPath());
            config = next;
            revision++;
            LOGGER.info("Loaded the shared Rider Power buff configuration");
        } catch (Exception e) {
            throw new IllegalStateException("KRC 通用 Buff 設定無效：" + e.getMessage(), e);
        }
    }
    public static void save(BoostConfig next, long expectedRevision) throws java.io.IOException {
        if (expectedRevision != revision) throw new IllegalArgumentException("設定已由另一位管理員更新，請重新開啟編輯器");
        BoostConfig.write(configPath(), next);
        config = next;
        revision++;
    }
}
