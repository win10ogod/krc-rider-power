package dev.krcboost.gametest;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import dev.krcboost.*;
import java.nio.file.Files;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("krcboost")
@PrefixGameTestTemplate(false)
public final class HeroRewardTests {
    private static final class TestPlayer extends Player {
        private boolean spectator;
        TestPlayer(ServerLevel level) { super(level, BlockPos.ZERO, 0, new GameProfile(UUID.randomUUID(), "hero-test")); }
        @Override public boolean isSpectator() { return spectator; }
        @Override public boolean isCreative() { return false; }
        void effectTick() { tickEffects(); }
    }
    private static void score(GameTestHelper helper, Player player, int target) {
        var store = KarmaSavedData.get(helper.getLevel().getServer());
        store.change(player.getUUID(), target - store.progress(player.getUUID()).score(),
                helper.getLevel().getServer().overworld().getGameTime(), new KarmaSettings(true, 200, 10, 1, 20, 10000));
    }
    private static void update(Player player) { NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player)); }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void reachingOneHundredGrantsHeroWithoutTransformation(GameTestHelper helper) {
        var player = new TestPlayer(helper.getLevel());
        score(helper, player, 99);
        update(player);
        helper.assertTrue(!player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE), "99 does not qualify");
        score(helper, player, 100);
        update(player);
        var hero = player.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
        helper.assertTrue(hero != null && hero.getAmplifier() == 0, "100 grants Hero I on the server tick");
        helper.assertTrue(!player.hasEffect(KrcBoost.RIDER_POWER), "Hero requires no transformation");
        for (int i = 0; i < 110; i++) { player.effectTick(); update(player); }
        helper.assertTrue(player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE), "Qualified players retain the reward");
        score(helper, player, 99);
        for (int i = 0; i < 200; i++) { player.effectTick(); update(player); }
        helper.assertTrue(!player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE), "Reward expires within 200 ticks below threshold");
        score(helper, player, -100);
        update(player);
        helper.assertTrue(!player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE), "Evil karma is not merit");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void externalHeroEffectsAreNeverShortenedDowngradedOrRemoved(GameTestHelper helper) {
        for (var original : new MobEffectInstance[]{
                new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 1200, 4),
                new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 1200, 0),
                new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, -1, 0)}) {
            var player = new TestPlayer(helper.getLevel());
            int duration = original.getDuration(), amplifier = original.getAmplifier();
            player.addEffect(original);
            score(helper, player, 100);
            update(player);
            score(helper, player, -100);
            update(player);
            var after = player.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
            helper.assertTrue(after != null && after.getDuration() == duration
                    && after.getAmplifier() == amplifier, "Existing hero remains intact above and below threshold");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void deadAndSpectatorPlayersDoNotGainHero(GameTestHelper helper) {
        var player = new TestPlayer(helper.getLevel());
        score(helper, player, 100);
        player.spectator = true;
        update(player);
        helper.assertTrue(!player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE), "Spectator cannot receive reward");
        player.spectator = false;
        player.setHealth(0);
        update(player);
        helper.assertTrue(!player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE), "Dead player cannot receive reward");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "hero_configuration", timeoutTicks = 100)
    public static void disabledKarmaStopsRenewalAndHigherScoresStillQualify(GameTestHelper helper) throws Exception {
        var path = KrcBoost.karmaConfigPath();
        String original = Files.readString(path);
        var player = new TestPlayer(helper.getLevel());
        score(helper, player, 150);
        try {
            var json = JsonParser.parseString(original).getAsJsonObject();
            json.addProperty("scoreLimit", 200);
            Files.writeString(path, json.toString());
            KrcBoost.reload();
            update(player);
            helper.assertTrue(player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE), "Scores above 100 also qualify with custom limit");
            json.addProperty("enabled", false);
            Files.writeString(path, json.toString());
            KrcBoost.reload();
            for (int i = 0; i < 200; i++) { player.effectTick(); update(player); }
            helper.assertTrue(!player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE), "Disabled karma stops renewing reward");
            player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 1200, 2));
            update(player);
            helper.assertTrue(player.getEffect(MobEffects.HERO_OF_THE_VILLAGE).getDuration() == 1200, "Disabled karma leaves external hero untouched");
        } finally {
            Files.writeString(path, original);
            KrcBoost.reload();
        }
        helper.succeed();
    }
}
