package dev.krcboost;

/** One power profile: good strengthens the shared bonus; evil trades health for damage. */
public record RiderPowerStats(boolean evil, double damageMultiplier, double healthMultiplier,
                              double secondaryBonusStrength) {
    public static RiderPowerStats calculate(BoostConfig base, KarmaSettings rules, int score) {
        if (!base.enabled()) return new RiderPowerStats(false, 1, 1, 0);
        int bounded = rules.bound(score);
        double secondary = rules.strength(bounded);
        if (rules.enabled() && bounded < 0) {
            double karma = -(double)bounded / rules.scoreLimit();
            double damageStrength = 1 + (rules.evilDamageBonusScale() - 1) * karma;
            return new RiderPowerStats(true, 1 + (base.attackMultiplier() - 1) * damageStrength,
                    base.healthMultiplier() * (1 - karma) + rules.evilHealthMultiplier() * karma, secondary);
        }
        return new RiderPowerStats(false, 1 + (base.attackMultiplier() - 1) * secondary,
                1 + (base.healthMultiplier() - 1) * secondary, secondary);
    }
}
