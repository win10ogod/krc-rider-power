package dev.krcboost;

/** One player's persistent progress. Time uses server game ticks, not time-of-day. */
public record KarmaProgress(int score, long windowStartedAt, int rewardedInWindow) {
    public static final long REWARD_WINDOW_TICKS = 24_000;
    public static KarmaProgress empty() { return new KarmaProgress(0, 0, 0); }
    public KarmaProgress change(int requested, long gameTime, KarmaSettings rules) {
        if (!rules.enabled() || requested == 0) return this;
        int current = rules.bound(score);
        long started = windowStartedAt;
        int rewarded = Math.max(0, rewardedInWindow);
        if (gameTime - started >= REWARD_WINDOW_TICKS) {
            started = gameTime;
            rewarded = 0;
        }
        int allowed = requested;
        if (requested > 0) allowed = Math.min(requested, Math.max(0, rules.dailyRewardLimit() - rewarded));
        int next = rules.bound((long)current + allowed);
        if (next > current) rewarded += next - current;
        return new KarmaProgress(next, started, rewarded);
    }
}
