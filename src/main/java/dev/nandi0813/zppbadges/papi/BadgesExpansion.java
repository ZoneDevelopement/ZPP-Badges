package dev.nandi0813.zppbadges.papi;

import dev.nandi0813.zppbadges.ZPPBadges;
import dev.nandi0813.zppbadges.data.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class BadgesExpansion extends PlaceholderExpansion {

    private static final String UNLOCKED_PREFIX = "unlocked_";

    private final ZPPBadges plugin;

    public BadgesExpansion(ZPPBadges plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "zppbadges";
    }

    @Override
    public String getAuthor() {
        return "Nandi0813";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return null;

        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        if (data == null) return "";

        return switch (params) {
            case "active" -> {
                Component active = plugin.getBadgeService().renderActive(player.getUniqueId());
                yield active == null ? "" : LegacyComponentSerializer.legacySection().serialize(active);
            }
            case "count" -> String.valueOf(unlockedCount(data));
            case "total" -> String.valueOf(plugin.getBadgeManager().size());
            case "active_count" -> String.valueOf(data.getActive().size());
            case "max" -> String.valueOf(maxActive(player));
            default -> params.startsWith(UNLOCKED_PREFIX)
                    ? String.valueOf(data.isUnlocked(params.substring(UNLOCKED_PREFIX.length()).toLowerCase(Locale.ROOT)))
                    : null;
        };
    }

    private long unlockedCount(PlayerData data) {
        return plugin.getBadgeManager().getBadges().stream()
                .filter(badge -> data.isUnlocked(badge.getId()))
                .count();
    }

    private int maxActive(OfflinePlayer player) {
        Player online = player.getPlayer();
        return online != null ? plugin.getBadgeService().getMaxActive(online) : plugin.getSettings().getMaxActive();
    }

}
