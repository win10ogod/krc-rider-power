package dev.krcboost.client;

import com.google.gson.JsonObject;
import dev.krcboost.BoostConfig;
import dev.krcboost.EditorProtocol;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** The server opens this screen only for administrators; save is re-authorized server-side. */
public final class RiderPowerScreen extends Screen {
    private static final String[] LABELS = {"傷害倍率", "生命倍率", "護甲加值", "護甲韌性加值", "移動速度倍率", "抗擊退加值（0～1）"};
    private final EditorProtocol.EditorData data;
    private final String[] values = new String[6];
    private final EditBox[] boxes = new EditBox[6];
    private boolean enabled;
    private String message;
    private int left, top, panelWidth, column;
    private Button toggle;

    public RiderPowerScreen(EditorProtocol.EditorData data) {
        super(Component.literal("騎士之力 · 通用 Buff"));
        this.data = data;
        this.message = data.message();
        load(data.config());
    }

    private void load(BoostConfig config) {
        enabled = config.enabled();
        double[] numbers = {config.attackMultiplier(), config.healthMultiplier(), config.armorBonus(),
                config.toughnessBonus(), config.speedMultiplier(), config.knockbackBonus()};
        for (int i = 0; i < values.length; i++) {
            values[i] = Double.toString(numbers[i]);
            if (boxes[i] != null) boxes[i].setValue(values[i]);
        }
        if (toggle != null) toggle.setMessage(toggleText());
    }

    private Component toggleText() { return Component.literal(enabled ? "Buff：開啟" : "Buff：停用"); }

    @Override protected void init() {
        panelWidth = Math.min(430, width - 28);
        column = (panelWidth - 16) / 2;
        left = (width - panelWidth) / 2;
        top = Math.max(0, (height - 238) / 2);
        for (int i = 0; i < 6; i++) {
            final int index = i;
            boxes[i] = new EditBox(font, left + (i % 2) * (column + 16), top + 62 + (i / 2) * 40,
                    column, 20, Component.literal(LABELS[i]));
            boxes[i].setMaxLength(32);
            boxes[i].setValue(values[i]);
            boxes[i].setResponder(value -> values[index] = value);
            boxes[i].setTooltip(Tooltip.create(Component.literal(i == 0 || i == 1 || i == 4
                    ? "1 表示原值，1.5 表示增加 50%。" : "在原有屬性上增加此數值。")));
            addRenderableWidget(boxes[i]);
        }
        toggle = addRenderableWidget(Button.builder(toggleText(), button -> {
            enabled = !enabled;
            button.setMessage(toggleText());
        }).bounds(left + panelWidth - 100, top + 25, 100, 20).build());
        int buttonWidth = (panelWidth - 16) / 3;
        addRenderableWidget(Button.builder(Component.literal("載入預設"), button -> load(BoostConfig.defaults()))
                .bounds(left, top + 194, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("儲存並套用全服"), button -> save())
                .bounds(left + buttonWidth + 8, top + 194, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("關閉"), button -> onClose())
                .bounds(left + 2 * (buttonWidth + 8), top + 194, buttonWidth, 20).build());
    }

    private void save() {
        try {
            double[] n = new double[6];
            for (int i = 0; i < 6; i++) n[i] = Double.parseDouble(values[i].trim());
            var config = new BoostConfig(enabled, n[0], n[1], n[2], n[3], n[4], n[5]);
            var request = new JsonObject();
            request.addProperty("revision", data.revision());
            request.add("config", EditorProtocol.GSON.toJsonTree(config));
            PacketDistributor.sendToServer(new EditorProtocol.SaveEditor(request.toString()));
            message = "正在由伺服器驗證並儲存…";
        } catch (Exception e) {
            message = "未儲存：" + (e instanceof NumberFormatException ? "請輸入有效的數字。" : e.getMessage());
        }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, top + 8, 0x73E0CB);
        graphics.drawString(font, "管理員設定 · 全服共用基礎值", left, top + 31, 0xD0D5DE, false);
        for (int i = 0; i < 6; i++)
            graphics.drawString(font, LABELS[i], left + (i % 2) * (column + 16), top + 50 + (i / 2) * 40, 0xFFFFFF, false);
        graphics.drawString(font, "完成變身時生效；解除變身移除，保留血量比例。", left, top + 168, 0xB2B8C5, false);
        graphics.drawString(font, "善惡調整實際加成；原有屬性上限仍適用。", left, top + 180, 0xB2B8C5, false);
        if (message != null && !message.isBlank())
            graphics.drawWordWrap(font, Component.literal(message), left, top + 220, panelWidth,
                    message.startsWith("未儲存") ? 0xFF9999 : 0x89E3BE);
    }
    @Override public boolean isPauseScreen() { return false; }
}
