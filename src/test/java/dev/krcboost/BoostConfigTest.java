package dev.krcboost;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

class BoostConfigTest {
    @TempDir Path directory;
    @Test void rejectsUnknownNonfiniteAndInvalidTypes() {
        for (String json : new String[]{"{\"attackMultiplier\":0}", "{\"attackMultiplier\":\"2\"}",
                "{\"healthMultiplier\":1e999}", "{\"enabled\":\"false\"}", "{\"armorBouns\":10}",
                "{\"knockbackBonus\":2}", "{\"speedMultiplier\":null}"}) {
            assertThrows(IllegalArgumentException.class, () -> BoostConfig.parse(JsonParser.parseString(json).getAsJsonObject()));
        }
    }
    @Test void highAdministratorValuesAreNotSilentlyReplaced() {
        var config = BoostConfig.parse(JsonParser.parseString("{\"attackMultiplier\":100000}").getAsJsonObject());
        assertEquals(100000, config.attackMultiplier());
    }
    @Test void validConfigurationRoundTripsWithoutChangingValues() throws Exception {
        Path path = directory.resolve("krcboost.json");
        var config = new BoostConfig(true, 2.5, 1.75, 6, 3, 1.2, 0.25);
        BoostConfig.write(path, config);
        assertEquals(config, BoostConfig.load(path));
        Files.writeString(path, "{\"healthMultiplier\": -2}");
        assertThrows(IllegalArgumentException.class, () -> BoostConfig.load(path));
        assertTrue(Files.readString(path).contains("-2"), "Bad config must not be overwritten with defaults");
    }
}
