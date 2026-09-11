package dev.krcboost;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/** Server-owned rules; absent fields use the documented defaults. */
public record KarmaSettings(boolean enabled, int scoreLimit, int cureReward, int hostileKillReward,
                            int villagerKillPenalty, int dailyRewardLimit,
                            double evilDamageBonusScale, double evilHealthMultiplier) {
    private static final Set<String> KEYS = Set.of("enabled", "scoreLimit", "cureReward", "hostileKillReward",
            "villagerKillPenalty", "dailyRewardLimit", "evilDamageBonusScale", "evilHealthMultiplier");
    public KarmaSettings {
        if (scoreLimit < 1 || cureReward < 0 || hostileKillReward < 0 || villagerKillPenalty < 0 || dailyRewardLimit < 0)
            throw new IllegalArgumentException("scoreLimit 須至少為 1，善惡事件分數與每日上限須為非負整數");
        if (!Double.isFinite(evilDamageBonusScale) || evilDamageBonusScale < 1)
            throw new IllegalArgumentException("evilDamageBonusScale 須為至少 1 的有限數字");
        if (!Double.isFinite(evilHealthMultiplier) || evilHealthMultiplier <= 0 || evilHealthMultiplier > 1)
            throw new IllegalArgumentException("evilHealthMultiplier 須大於 0 且不超過 1");
    }
    public KarmaSettings(boolean enabled, int scoreLimit, int cureReward, int hostileKillReward,
                         int villagerKillPenalty, int dailyRewardLimit) {
        this(enabled, scoreLimit, cureReward, hostileKillReward, villagerKillPenalty, dailyRewardLimit, 3.0, 0.5);
    }
    public static KarmaSettings defaults() { return new KarmaSettings(true, 100, 10, 1, 20, 30); }
    public int bound(long score) { return (int)Math.max(-((long)scoreLimit), Math.min(scoreLimit, score)); }
    /** Shared scale for secondary bonuses; evil damage and health use RiderPowerStats. */
    public double strength(int score) { return enabled ? 1.0 + (double)bound(score) / scoreLimit : 1.0; }
    public static KarmaSettings load(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path.toAbsolutePath().getParent());
            Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(defaults()) + "\n");
        }
        try (var reader = Files.newBufferedReader(path)) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject());
        }
    }
    public static KarmaSettings parse(JsonObject json) {
        for (String key : json.keySet())
            if (!KEYS.contains(key)) throw new IllegalArgumentException("未知善惡設定：" + key);
        var d = defaults();
        boolean enabled = d.enabled;
        if (json.has("enabled")) {
            var value = json.get("enabled");
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean())
                throw new IllegalArgumentException("enabled 必須是布林值");
            enabled = value.getAsBoolean();
        }
        return new KarmaSettings(enabled, number(json, "scoreLimit", d.scoreLimit),
                number(json, "cureReward", d.cureReward), number(json, "hostileKillReward", d.hostileKillReward),
                number(json, "villagerKillPenalty", d.villagerKillPenalty),
                number(json, "dailyRewardLimit", d.dailyRewardLimit),
                decimal(json, "evilDamageBonusScale", d.evilDamageBonusScale),
                decimal(json, "evilHealthMultiplier", d.evilHealthMultiplier));
    }
    private static double decimal(JsonObject json, String name, double fallback) {
        if (!json.has(name)) return fallback;
        var value = json.get(name);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException(name + " 必須是數字");
        return value.getAsDouble();
    }
    private static int number(JsonObject json, String name, int fallback) {
        if (!json.has(name)) return fallback;
        var value = json.get(name);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException(name + " 必須是整數");
        try { return value.getAsBigDecimal().intValueExact(); }
        catch (ArithmeticException e) { throw new IllegalArgumentException(name + " 必須是有效整數", e); }
    }
}
