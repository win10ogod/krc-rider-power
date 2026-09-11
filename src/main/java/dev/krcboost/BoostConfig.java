package dev.krcboost;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.*;
import java.util.Set;

/** One server-owned set of values for every transformed KRC player. */
public record BoostConfig(boolean enabled, double attackMultiplier, double healthMultiplier,
                          double armorBonus, double toughnessBonus, double speedMultiplier, double knockbackBonus) {
    private static final Set<String> KEYS = Set.of("enabled", "attackMultiplier", "healthMultiplier",
            "armorBonus", "toughnessBonus", "speedMultiplier", "knockbackBonus");
    public BoostConfig {
        check("attackMultiplier", attackMultiplier, 1);
        check("healthMultiplier", healthMultiplier, 1);
        check("armorBonus", armorBonus, 0);
        check("toughnessBonus", toughnessBonus, 0);
        check("speedMultiplier", speedMultiplier, 1);
        check("knockbackBonus", knockbackBonus, 0);
        if (knockbackBonus > 1) throw new IllegalArgumentException("抗擊退加值須介於 0～1");
    }
    private static void check(String name, double value, double minimum) {
        if (!Double.isFinite(value) || value < minimum)
            throw new IllegalArgumentException(name + " 必須是 >= " + minimum + " 的有限數字");
    }
    public static BoostConfig defaults() { return new BoostConfig(true, 1.5, 1.5, 4, 2, 1.15, 0.1); }
    public static BoostConfig load(Path path) throws IOException {
        if (!Files.exists(path)) write(path, defaults());
        try (var reader = Files.newBufferedReader(path)) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject());
        }
    }
    public static void write(Path path, BoostConfig config) throws IOException {
        Path absolute = path.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        Path temporary = Files.createTempFile(absolute.getParent(), "krcboost-", ".tmp");
        try {
            Files.writeString(temporary, new GsonBuilder().setPrettyPrinting().create().toJson(config) + "\n");
            try {
                Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporary); }
    }
    public static BoostConfig parse(JsonObject json) {
        for (String key : json.keySet())
            if (!KEYS.contains(key)) throw new IllegalArgumentException("未知設定：" + key);
        var d = defaults();
        boolean enabled = d.enabled;
        if (json.has("enabled")) {
            var value = json.get("enabled");
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean())
                throw new IllegalArgumentException("enabled 必須是布林值");
            enabled = value.getAsBoolean();
        }
        return new BoostConfig(enabled, number(json, "attackMultiplier", d.attackMultiplier),
                number(json, "healthMultiplier", d.healthMultiplier), number(json, "armorBonus", d.armorBonus),
                number(json, "toughnessBonus", d.toughnessBonus), number(json, "speedMultiplier", d.speedMultiplier),
                number(json, "knockbackBonus", d.knockbackBonus));
    }
    private static double number(JsonObject json, String name, double fallback) {
        if (!json.has(name)) return fallback;
        var value = json.get(name);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException(name + " 必須是數字");
        return value.getAsDouble();
    }
}
