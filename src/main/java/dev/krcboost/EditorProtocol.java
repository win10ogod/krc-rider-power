package dev.krcboost;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class EditorProtocol {
    public static final Gson GSON = new Gson();
    public record EditorData(long revision, BoostConfig config, String message) {}
    public record OpenEditor(String json) implements CustomPacketPayload {
        public static final Type<OpenEditor> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(KrcBoost.ID, "open_editor"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenEditor> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, OpenEditor::json, OpenEditor::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record SaveEditor(String json) implements CustomPacketPayload {
        public static final Type<SaveEditor> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(KrcBoost.ID, "save_editor"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SaveEditor> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, SaveEditor::json, SaveEditor::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToClient(OpenEditor.TYPE, OpenEditor.CODEC,
                (packet, context) -> context.enqueueWork(() -> dev.krcboost.client.EditorClient.open(packet.json())));
        registrar.playToServer(SaveEditor.TYPE, SaveEditor.CODEC, (packet, context) -> context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!player.hasPermissions(2)) {
                player.sendSystemMessage(Component.literal("只有管理員可以修改騎士之力。"));
                return;
            }
            try {
                var root = JsonParser.parseString(packet.json()).getAsJsonObject();
                for (String key : root.keySet())
                    if (!key.equals("revision") && !key.equals("config")) throw new IllegalArgumentException("未知欄位：" + key);
                apply(player, BoostConfig.parse(root.getAsJsonObject("config")), root.get("revision").getAsLong());
                open(player, "已儲存，全服共用基礎值；實際加成依各玩家善惡調整。");
            } catch (Exception e) {
                open(player, "未儲存：" + e.getMessage());
            }
        }));
    }
    public static void apply(ServerPlayer player, BoostConfig next, long revision) throws java.io.IOException {
        if (!player.hasPermissions(2)) throw new IllegalArgumentException("只有管理員可以修改騎士之力");
        KrcBoost.save(next, revision);
        refresh(player.server);
    }
    public static void refresh(net.minecraft.server.MinecraftServer server) {
        for (var player : server.getPlayerList().getPlayers()) BoostEngine.update(player, KrcBoost.config());
    }
    public static void open(ServerPlayer player, String message) {
        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("只有管理員可以修改騎士之力；使用 /krcboost status 查看目前加成。"));
            return;
        }
        PacketDistributor.sendToPlayer(player, new OpenEditor(GSON.toJson(new EditorData(KrcBoost.revision(), KrcBoost.config(), message))));
    }
}
