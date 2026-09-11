package dev.krcboost.client;

import dev.krcboost.EditorProtocol;
import net.minecraft.client.Minecraft;

public final class EditorClient {
    public static void open(String json) {
        var data = EditorProtocol.GSON.fromJson(json, EditorProtocol.EditorData.class);
        Minecraft.getInstance().setScreen(new RiderPowerScreen(data));
    }
}
