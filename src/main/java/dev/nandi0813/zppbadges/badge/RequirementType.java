package dev.nandi0813.zppbadges.badge;

public enum RequirementType {
    WINS,
    RANKED_WINS,
    KILLS,
    ELO,
    EXPERIENCE,
    BEST_WIN_STREAK,
    BEST_KILL_STREAK,
    MATCHES_PLAYED,
    DAYS_SINCE_JOIN,
    EVENT_WINS,
    FFA_KILLS,
    DIVISION,
    LEADERBOARD_TOP;

    public String messageKey() {
        return "REQUIREMENT." + name().replace('_', '-');
    }

}
