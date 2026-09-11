package dev.krcboost;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Stored in overworld data, shared across dimensions and retained after player death. */
public final class KarmaSavedData extends SavedData {
    private static final Factory<KarmaSavedData> FACTORY = new Factory<>(KarmaSavedData::new, KarmaSavedData::load);
    private final Map<UUID, KarmaProgress> players = new HashMap<>();
    public static KarmaSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "krcboost_karma");
    }
    public KarmaProgress progress(UUID player) { return players.getOrDefault(player, KarmaProgress.empty()); }
    public int change(UUID player, int points, long gameTime, KarmaSettings rules) {
        if (!rules.enabled() || points == 0) return 0;
        var previous = progress(player);
        var next = previous.change(points, gameTime, rules);
        if (!next.equals(previous)) {
            players.put(player, next);
            setDirty();
        }
        return next.score() - rules.bound(previous.score());
    }
    public static KarmaSavedData load(CompoundTag root, HolderLookup.Provider registries) {
        var data = new KarmaSavedData();
        for (Tag entry : root.getList("Players", Tag.TAG_COMPOUND)) {
            var tag = (CompoundTag)entry;
            if (!tag.hasUUID("Player")) throw new IllegalArgumentException("善惡紀錄缺少玩家 UUID");
            data.players.put(tag.getUUID("Player"), new KarmaProgress(tag.getInt("Score"),
                    tag.getLong("RewardWindowStart"), tag.getInt("RewardedInWindow")));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
        var entries = new ListTag();
        players.forEach((id, progress) -> {
            var entry = new CompoundTag();
            entry.putUUID("Player", id);
            entry.putInt("Score", progress.score());
            entry.putLong("RewardWindowStart", progress.windowStartedAt());
            entry.putInt("RewardedInWindow", progress.rewardedInWindow());
            entries.add(entry);
        });
        root.put("Players", entries);
        return root;
    }
}
