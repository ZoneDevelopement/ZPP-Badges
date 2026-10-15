package dev.nandi0813.zppbadges.badge;

import dev.nandi0813.api.Enum.DivisionName;
import dev.nandi0813.api.Enum.LeaderboardType;
import dev.nandi0813.api.Enum.WeightClass;
import dev.nandi0813.api.ZonePracticeApi;
import dev.nandi0813.zppbadges.config.Messages;
import dev.nandi0813.zppbadges.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public final class Requirement {

    private static final long DAY_MILLIS = 86_400_000L;
    private static final Pattern LEGACY = Pattern.compile("(?i)[&§](#[0-9a-f]{6}|[0-9a-fk-or])");

    private final RequirementType type;
    private final long amount;
    private final String ladder;
    private final String division;
    private final LeaderboardType stat;

    private Requirement(RequirementType type, long amount, String ladder, String division, LeaderboardType stat) {
        this.type = type;
        this.amount = amount;
        this.ladder = ladder;
        this.division = division;
        this.stat = stat;
    }

    public static Requirement parse(Map<?, ?> map, Logger logger, String badgeId) {
        RequirementType type = parseEnum(RequirementType.class, map.get("TYPE"));
        if (type == null) {
            logger.warning("Badge " + badgeId + " has an invalid requirement TYPE, skipping the requirement.");
            return null;
        }

        String ladder = optionalString(map.get("LADDER"));
        String division = optionalString(map.get("DIVISION"));
        LeaderboardType stat = parseEnum(LeaderboardType.class, map.get("STAT"));

        long amount = 0;
        if (type != RequirementType.DIVISION) {
            Long parsed = parseLong(map.get("AMOUNT"));
            if (parsed == null) {
                logger.warning("Badge " + badgeId + " has a " + type.name() + " requirement without a valid AMOUNT, skipping the requirement.");
                return null;
            }
            amount = parsed;
        }

        if (type == RequirementType.ELO && ladder == null) {
            logger.warning("Badge " + badgeId + " has an ELO requirement without LADDER, skipping the requirement.");
            return null;
        }
        if (type == RequirementType.DIVISION && division == null) {
            logger.warning("Badge " + badgeId + " has a DIVISION requirement without DIVISION, skipping the requirement.");
            return null;
        }
        if (type == RequirementType.LEADERBOARD_TOP && stat == null) {
            logger.warning("Badge " + badgeId + " has a LEADERBOARD_TOP requirement without a valid STAT, skipping the requirement.");
            return null;
        }

        return new Requirement(type, amount, ladder, division, stat);
    }

    public long current(Player player, PlayerData data) {
        ZonePracticeApi api = ZonePracticeApi.getInstance();
        return switch (type) {
            case WINS -> ladder != null
                    ? positive(api.getLadderWins(player, ladder, WeightClass.UNRANKED)) + positive(api.getLadderWins(player, ladder, WeightClass.RANKED))
                    : positive(api.getGlobalWins(player));
            case RANKED_WINS -> ladder != null
                    ? positive(api.getLadderWins(player, ladder, WeightClass.RANKED))
                    : api.getLadderNames().stream().mapToLong(name -> positive(api.getLadderWins(player, name, WeightClass.RANKED))).sum();
            case KILLS -> positive(ladder != null ? api.getLadderKills(player, ladder) : api.getGlobalKills(player));
            case ELO -> positive(api.getElo(player, ladder));
            case EXPERIENCE -> positive(api.getExperience(player));
            case BEST_WIN_STREAK -> ladder != null
                    ? Math.max(positive(api.getLadderBestWinStreak(player, ladder, WeightClass.UNRANKED)), positive(api.getLadderBestWinStreak(player, ladder, WeightClass.RANKED)))
                    : positive(api.getBestWinStreak(player));
            case BEST_KILL_STREAK -> positive(ladder != null ? api.getLadderBestKillStreak(player, ladder) : api.getBestKillStreak(player));
            case MATCHES_PLAYED -> positive(api.getMatchesPlayed(player));
            case DAYS_SINCE_JOIN -> daysSince(api.getFirstJoin(player));
            case EVENT_WINS -> data.getEventWins();
            case FFA_KILLS -> data.getFfaKills();
            case DIVISION -> divisionIndex(api.getPlayerDivision(player, DivisionName.FULL));
            case LEADERBOARD_TOP -> api.getLeaderboardPosition(player, stat, ladder);
        };
    }

    public long target() {
        if (type != RequirementType.DIVISION) return amount;

        long index = divisionIndex(division);
        return index == 0 ? Long.MAX_VALUE : index;
    }

    public boolean isMet(Player player, PlayerData data) {
        long current = current(player, data);
        if (type == RequirementType.LEADERBOARD_TOP) return current > 0 && current <= amount;
        return current >= target();
    }

    public Component describe(Messages messages) {
        return messages.get(type.messageKey(),
                Placeholder.unparsed("target", String.valueOf(amount)),
                Placeholder.component("ladder_suffix", ladder == null ? Component.empty() : messages.get("REQUIREMENT.LADDER-SUFFIX", Placeholder.unparsed("ladder", ladder))),
                Placeholder.parsed("division", division == null ? "" : division),
                Placeholder.unparsed("stat", stat == null ? "" : stat.name()));
    }

    public Component progressLine(Player player, PlayerData data, Messages messages) {
        if (isMet(player, data)) {
            return messages.get("REQUIREMENT.DONE", Placeholder.component("requirement", describe(messages)));
        }

        Component currentComponent;
        Component targetComponent;
        if (type == RequirementType.LEADERBOARD_TOP) {
            long position = current(player, data);
            currentComponent = position > 0
                    ? messages.get("REQUIREMENT.LEADERBOARD-POSITION", Placeholder.unparsed("position", String.valueOf(position)))
                    : messages.get("REQUIREMENT.LEADERBOARD-UNRANKED");
            targetComponent = Component.text("#" + amount);
        } else if (type == RequirementType.DIVISION) {
            String playerDivision = ZonePracticeApi.getInstance().getPlayerDivision(player, DivisionName.FULL);
            currentComponent = playerDivision == null ? Component.text("-") : parseDivision(playerDivision);
            targetComponent = parseDivision(division);
        } else {
            currentComponent = Component.text(String.valueOf(current(player, data)));
            targetComponent = Component.text(String.valueOf(amount));
        }

        return messages.get("REQUIREMENT.PROGRESS",
                Placeholder.component("requirement", describe(messages)),
                Placeholder.component("current", currentComponent),
                Placeholder.component("target", targetComponent));
    }

    private static long positive(long value) {
        return Math.max(0, value);
    }

    private static long daysSince(long firstJoin) {
        return firstJoin <= 0 ? 0 : (System.currentTimeMillis() - firstJoin) / DAY_MILLIS;
    }

    private static long divisionIndex(String name) {
        if (name == null) return 0;

        String wanted = plain(name);
        List<String> names = ZonePracticeApi.getInstance().getDivisionNames(DivisionName.FULL);
        for (int i = 0; i < names.size(); i++) {
            if (plain(names.get(i)).equalsIgnoreCase(wanted)) return i + 1L;
        }
        return 0;
    }

    static Component parseDivision(String raw) {
        return MiniMessage.miniMessage().deserialize(LEGACY.matcher(raw).replaceAll(""));
    }

    private static String plain(String miniMessage) {
        return PlainTextComponentSerializer.plainText().serialize(parseDivision(miniMessage));
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> enumClass, Object raw) {
        if (raw == null) return null;

        try {
            return Enum.valueOf(enumClass, String.valueOf(raw).trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String optionalString(Object raw) {
        if (raw == null) return null;

        String value = String.valueOf(raw).trim();
        return value.isEmpty() ? null : value;
    }

    private static Long parseLong(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Number number) return number.longValue();

        try {
            return Long.parseLong(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
