package dev.krcboost;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RiderPowerStatsTest {
    @Test void evilTradesHealthForDamageAndGoodRetainsItsBonuses() {
        var base = BoostConfig.defaults();
        var rules = KarmaSettings.defaults();
        var neutral = RiderPowerStats.calculate(base, rules, 0);
        assertEquals(new RiderPowerStats(false, 1.5, 1.5, 1), neutral);
        assertEquals(new RiderPowerStats(false, 2, 2, 2), RiderPowerStats.calculate(base, rules, 100));
        assertEquals(new RiderPowerStats(true, 2, 1, 0.5), RiderPowerStats.calculate(base, rules, -50));
        assertEquals(new RiderPowerStats(true, 2.5, 0.5, 0), RiderPowerStats.calculate(base, rules, -100));
        double previousDamage = neutral.damageMultiplier(), previousHealth = neutral.healthMultiplier();
        for (int score = -1; score >= -100; score--) {
            var power = RiderPowerStats.calculate(base, rules, score);
            assertTrue(power.evil());
            assertTrue(power.damageMultiplier() > previousDamage);
            assertTrue(power.healthMultiplier() < previousHealth && power.healthMultiplier() > 0);
            previousDamage = power.damageMultiplier(); previousHealth = power.healthMultiplier();
        }
    }
    @Test void customEndpointsAndScoreLimitsAreHonoredWithoutReducingBaseConfiguration() {
        var rules = new KarmaSettings(true, 200, 10, 1, 20, 30, 5, 0.25);
        var base = new BoostConfig(true, 10, 4, 50, 30, 2, 1);
        assertEquals(new RiderPowerStats(true, 46, 0.25, 0), RiderPowerStats.calculate(base, rules, -200));
        assertEquals(new RiderPowerStats(true, 28, 2.125, 0.5), RiderPowerStats.calculate(base, rules, -100));
        assertEquals(new RiderPowerStats(false, 19, 7, 2), RiderPowerStats.calculate(base, rules, Integer.MAX_VALUE));
        assertEquals(RiderPowerStats.calculate(base, rules, -200), RiderPowerStats.calculate(base, rules, Integer.MIN_VALUE));
    }
    @Test void disablingKarmaRestoresBaseValuesAndDisablingBuffRemovesBothProfiles() {
        var disabledKarma = new KarmaSettings(false, 100, 10, 1, 20, 30);
        assertEquals(new RiderPowerStats(false, 1.5, 1.5, 1),
                RiderPowerStats.calculate(BoostConfig.defaults(), disabledKarma, -100));
        var disabledBuff = new BoostConfig(false, 1.5, 1.5, 4, 2, 1.15, 0.1);
        assertEquals(new RiderPowerStats(false, 1, 1, 0),
                RiderPowerStats.calculate(disabledBuff, KarmaSettings.defaults(), -100));
    }
    @Test void oldKarmaFilesUseNewDefaultsAndInvalidEvilRulesAreRejected() {
        var old = JsonParser.parseString("{\"enabled\":true,\"scoreLimit\":100,\"cureReward\":10,\"hostileKillReward\":1,\"villagerKillPenalty\":20,\"dailyRewardLimit\":30}").getAsJsonObject();
        assertEquals(KarmaSettings.defaults(), KarmaSettings.parse(old));
        for (String json : new String[]{"{\"evilDamageBonusScale\":0.5}", "{\"evilDamageBonusScale\":1e400}",
                "{\"evilDamageBonusScale\":\"3\"}", "{\"evilHealthMultiplier\":0}",
                "{\"evilHealthMultiplier\":-0.5}", "{\"evilHealthMultiplier\":1.1}", "{\"evilHealthMultiplier\":null}"})
            assertThrows(IllegalArgumentException.class, () -> KarmaSettings.parse(JsonParser.parseString(json).getAsJsonObject()));
        var custom = KarmaSettings.parse(JsonParser.parseString("{\"evilDamageBonusScale\":4.5,\"evilHealthMultiplier\":0.75}").getAsJsonObject());
        assertEquals(4.5, custom.evilDamageBonusScale());
        assertEquals(0.75, custom.evilHealthMultiplier());
    }
}
