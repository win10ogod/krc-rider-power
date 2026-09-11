package dev.krcboost;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = KrcBoost.ID)
public final class BoostCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("krcboost")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("騎士之力：/krcboost status；善惡值：/krcboost karma；管理員編輯：/krcboost edit"), false);
                    return 1;
                })
                .then(Commands.literal("edit").requires(source -> source.hasPermission(2)).executes(context -> {
                    EditorProtocol.open(context.getSource().getPlayerOrException(), "");
                    return 1;
                }))
                .then(Commands.literal("status").executes(context -> {
                    var player = context.getSource().getPlayer();
                    var c = KrcBoost.config();
                    var text = "騎士之力基礎設定 " + (c.enabled() ? "已開啟" : "已停用")
                            + "；攻擊 ×" + c.attackMultiplier() + "；生命 ×" + c.healthMultiplier()
                            + "；護甲 +" + c.armorBonus() + "；韌性 +" + c.toughnessBonus()
                            + "；移速 ×" + c.speedMultiplier() + "；抗擊退 +" + c.knockbackBonus()
                            + (player == null ? "" : "\n目前資格：" + (BoostEngine.transformed(player) ? "已完成 KRC 變身" : "未完成 KRC 變身")
                            + "；善惡值：" + KarmaSystem.score(player) + "；加成強度：" + Math.round(KarmaSystem.strength(player) * 100) + "%"
                            + "；符合變身資格時傷害 ×" + (c.enabled() ? BoostEngine.damageMultiplier(player, c) : 1));
                    context.getSource().sendSuccess(() -> Component.literal(text), false);
                    return 1;
                }))
                .then(Commands.literal("karma").executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    var rules = KrcBoost.karmaSettings();
                    context.getSource().sendSuccess(() -> Component.literal("善惡系統" + (rules.enabled() ? "已開啟" : "已停用（保留紀錄）")
                            + "；善惡值：" + KarmaSystem.score(player) + "（範圍 -" + rules.scoreLimit() + "～+" + rules.scoreLimit() + "）"
                            + "；騎士之力加成強度：" + Math.round(KarmaSystem.strength(player) * 100) + "%"
                            + "\n首次救治未由村民感染而來的殭屍村民 +" + rules.cureReward()
                            + "；擊殺敵對怪物 +" + rules.hostileKillReward()
                            + "；殺害村民 -" + rules.villagerKillPenalty()
                            + "；每 24000 遊戲 tick 最多獲得 " + rules.dailyRewardLimit() + " 正分。"), false);
                    return 1;
                }))
                .then(Commands.literal("reload").requires(source -> source.hasPermission(2)).executes(context -> {
                    try {
                        KrcBoost.reload();
                        EditorProtocol.refresh(context.getSource().getServer());
                        context.getSource().sendSuccess(() -> Component.literal("已重新載入通用 Buff 與善惡設定。"), false);
                        return 1;
                    } catch (Exception e) {
                        context.getSource().sendFailure(Component.literal("載入失敗，沿用先前設定：" + e.getMessage()));
                        return 0;
                    }
                })));
    }
}
