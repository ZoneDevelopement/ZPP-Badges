package dev.nandi0813.zppbadges.badge;

import dev.nandi0813.api.ZonePracticeApi;
import dev.nandi0813.zppbadges.ZPPBadges;
import dev.nandi0813.zppbadges.config.Messages;
import dev.nandi0813.zppbadges.config.Settings;
import dev.nandi0813.zppbadges.data.PlayerData;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BadgeService {

    private final ZPPBadges plugin;

    public BadgeService(ZPPBadges plugin) {
        this.plugin = plugin;
    }

    public int getMaxActive(Player player) {
        Settings settings = plugin.getSettings();
        int max = settings.getMaxActive();
        for (int n = settings.getMaxPermissionLimit(); n >= 1; n--) {
            if (player.hasPermission("zppbadges.max." + n)) {
                max = Math.max(max, n);
                break;
            }
        }
        return max;
    }

    public void checkUnlocks(Player player) {
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        if (data == null) return;

        for (Badge badge : plugin.getBadgeManager().getBadges()) {
            if (badge.isAdminOnly() || data.isUnlocked(badge.getId())) continue;

            if (badge.getRequirements().stream().allMatch(requirement -> requirement.isMet(player, data))) {
                unlock(player, data, badge, true);
            }
        }
    }

    public boolean unlock(Player player, PlayerData data, Badge badge, boolean notify) {
        if (!data.getUnlocked().add(badge.getId())) return false;
        data.setDirty(true);

        if (notify) {
            plugin.getMessages().send(player, "UNLOCKED", Placeholder.component("badge_name", badge.getName()));

            String soundKey = plugin.getSettings().getUnlockSound();
            if (!soundKey.isEmpty()) {
                try {
                    player.playSound(Sound.sound(Key.key(soundKey), Sound.Source.MASTER, 1f, 1f));
                } catch (RuntimeException ignored) {
                }
            }
        }
        return true;
    }

    public void toggle(Player player, Badge badge) {
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        if (data == null) return;

        Messages messages = plugin.getMessages();

        if (!data.isUnlocked(badge.getId())) {
            messages.send(player, "LOCKED");
            return;
        }

        if (data.isActive(badge.getId())) {
            data.getActive().remove(badge.getId());
            messages.send(player, "DEACTIVATED", Placeholder.component("badge_name", badge.getName()));
        } else {
            int max = getMaxActive(player);
            if (data.getActive().size() >= max) {
                messages.send(player, "MAX-REACHED", Placeholder.unparsed("max", String.valueOf(max)));
                return;
            }
            data.getActive().add(badge.getId());
            messages.send(player, "ACTIVATED", Placeholder.component("badge_name", badge.getName()));
        }

        data.setDirty(true);
        plugin.getDataManager().saveAsync(data);
        refreshName(player);
    }

    public void clearActive(Player player) {
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        if (data == null) return;

        data.getActive().clear();
        plugin.getMessages().send(player, "CLEARED");
        plugin.getDataManager().saveAsync(data);
        refreshName(player);
    }

    public void trimActive(Player player) {
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        if (data == null) return;

        boolean changed = data.getActive().removeIf(id -> !data.isUnlocked(id) || plugin.getBadgeManager().get(id) == null);
        int max = getMaxActive(player);
        while (data.getActive().size() > max) {
            data.getActive().remove(data.getActive().size() - 1);
            changed = true;
        }

        if (changed) {
            data.setDirty(true);
            refreshName(player);
        }
    }

    public boolean give(Player target, Badge badge) {
        PlayerData data = plugin.getDataManager().get(target.getUniqueId());
        if (data == null) return false;
        if (!unlock(target, data, badge, true)) return false;

        plugin.getDataManager().saveAsync(data);
        return true;
    }

    public boolean take(Player target, Badge badge) {
        PlayerData data = plugin.getDataManager().get(target.getUniqueId());
        if (data == null) return false;

        boolean removed = data.getUnlocked().remove(badge.getId());
        if (data.getActive().remove(badge.getId())) removed = true;
        if (!removed) return false;

        plugin.getDataManager().saveAsync(data);
        refreshName(target);
        return true;
    }

    public boolean reset(Player target) {
        PlayerData data = plugin.getDataManager().get(target.getUniqueId());
        if (data == null) return false;

        boolean changed = !data.getUnlocked().isEmpty() || !data.getActive().isEmpty() || data.getEventWins() != 0 || data.getFfaKills() != 0;
        if (!changed) return false;

        data.getUnlocked().clear();
        data.getActive().clear();
        data.setEventWins(0);
        data.setFfaKills(0);
        plugin.getDataManager().saveAsync(data);
        refreshName(target);
        return true;
    }

    public Component renderActive(UUID uuid) {
        PlayerData data = plugin.getDataManager().get(uuid);
        if (data == null || data.getActive().isEmpty()) return null;

        List<Component> displays = new ArrayList<>();
        for (String id : data.getActive()) {
            Badge badge = plugin.getBadgeManager().get(id);
            if (badge != null) displays.add(badge.getDisplay());
        }
        if (displays.isEmpty()) return null;

        return Component.join(JoinConfiguration.separator(plugin.getSettings().getBadgeSeparator()), displays);
    }

    public void refreshName(Player player) {
        ZonePracticeApi.getInstance().getNameDecorationRegistry().refresh(player);
    }

}
