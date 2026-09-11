package dev.krcboost.gametest.client;

import dev.krcboost.BoostConfig;
import dev.krcboost.EditorProtocol;
import dev.krcboost.client.RiderPowerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.Files;
import java.nio.file.Path;

/** Development-only isolated UI verification; this source set is absent from the release jar. */
@EventBusSubscriber(modid = "krcboost_tests", value = Dist.CLIENT)
public final class UiSmokeTest {
    private static int ticks;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("krcboost.uiSmoke")) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.getOverlay() != null || minecraft.screen == null) return;
        if (ticks++ == 0) {
            minecraft.setScreen(new RiderPowerScreen(new EditorProtocol.EditorData(0, BoostConfig.defaults(), "管理員專用 · 伺服器驗證後才會儲存")));
        } else if (ticks == 40) {
            try {
                Path output = Path.of("../evidence/rider-power-editor.png");
                Files.createDirectories(output.toAbsolutePath().getParent());
                try (var image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
                    image.writeToFile(output);
                }
                System.out.println("KRC_BUFF_UI_SMOKE_OK " + output.toAbsolutePath());
                minecraft.stop();
            } catch (Exception e) { throw new IllegalStateException("UI capture failed", e); }
        }
    }
}
