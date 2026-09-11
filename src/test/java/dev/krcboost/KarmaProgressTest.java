package dev.krcboost;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class KarmaProgressTest {
    @TempDir Path directory;
    @Test void neutralGoodAndEvilScaleOnlyBonusStrength() {
        var rules = KarmaSettings.defaults();
        assertEquals(1, rules.strength(0));
        assertEquals(2, rules.strength(100));
        assertEquals(0, rules.strength(-100));
        assertEquals(1.1, rules.strength(10));
        assertEquals(2, rules.strength(Integer.MAX_VALUE));
        assertEquals(0, rules.strength(Integer.MIN_VALUE));
    }
    @Test void positiveBudgetSurvivesEvilActionsAndDoesNotResetWithTimeOfDay() {
        var rules = KarmaSettings.defaults();
        var progress = KarmaProgress.empty();
        for (int i = 0; i < 500; i++) progress = progress.change(1, 500, rules);
        assertEquals(30, progress.score());
        assertEquals(30, progress.rewardedInWindow());
        progress = progress.change(-20, 500, rules).change(10, 500, rules);
        assertEquals(10, progress.score(), "An evil action does not refill the reward budget");
        progress = progress.change(10, 0, rules);
        assertEquals(10, progress.score(), "Moving a clock backwards cannot refill rewards");
        progress = progress.change(10, 24000, rules);
        assertEquals(20, progress.score());
        assertEquals(10, progress.rewardedInWindow());
    }
    @Test void scoreIsBoundedAndDisablingPreservesEarnedHistory() {
        var rules = KarmaSettings.defaults();
        var progress = KarmaProgress.empty().change(Integer.MIN_VALUE, 0, rules);
        assertEquals(-100, progress.score());
        var disabled = new KarmaSettings(false, 100, 10, 1, 20, 30);
        assertEquals(progress, progress.change(100, 24000, disabled));
        assertEquals(1, disabled.strength(-100));
        var large = new KarmaSettings(true, Integer.MAX_VALUE, 10, 1, 20, Integer.MAX_VALUE);
        assertEquals(0, new KarmaProgress(-Integer.MAX_VALUE, 0, 0).change(Integer.MAX_VALUE, 0, large).score());
    }
    @Test void settingsRejectUnknownFieldsFractionsAndInvalidValues() {
        for (String json : new String[]{"{\"scoreLimit\":0}", "{\"cureReward\":-1}",
                "{\"hostileKillReward\":1.5}", "{\"villagerKillPenalty\":\"20\"}",
                "{\"dailyRewardLimit\":1e100}", "{\"enabled\":1}", "{\"score\":100}"}) {
            assertThrows(IllegalArgumentException.class, () -> KarmaSettings.parse(JsonParser.parseString(json).getAsJsonObject()));
        }
        assertEquals(KarmaSettings.defaults(), KarmaSettings.parse(JsonParser.parseString("{}").getAsJsonObject()));
    }
    @Test void ruleFileUsesDefaultsWithoutOverwritingInvalidFiles() throws Exception {
        Path file = directory.resolve("karma.json");
        assertEquals(KarmaSettings.defaults(), KarmaSettings.load(file));
        Files.writeString(file, "{\"hostileKillReward\":5}");
        assertEquals(5, KarmaSettings.load(file).hostileKillReward());
        Files.writeString(file, "{\"villagerKillPenalty\":-1}");
        assertThrows(IllegalArgumentException.class, () -> KarmaSettings.load(file));
        assertEquals("{\"villagerKillPenalty\":-1}", Files.readString(file));
    }
}
