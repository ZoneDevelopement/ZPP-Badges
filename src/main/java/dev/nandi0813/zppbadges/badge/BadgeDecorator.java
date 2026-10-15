package dev.nandi0813.zppbadges.badge;

import dev.nandi0813.api.Enum.NameLocation;
import dev.nandi0813.api.Interface.NameDecorator;
import dev.nandi0813.zppbadges.ZPPBadges;
import dev.nandi0813.zppbadges.config.Settings;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public final class BadgeDecorator implements NameDecorator {

    private final ZPPBadges plugin;
    private final BadgeService service;

    public BadgeDecorator(ZPPBadges plugin, BadgeService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public String getId() {
        return "zppbadges";
    }

    @Override
    public Component getPrefix(Player player, NameLocation location) {
        Settings.LocationSettings settings = plugin.getSettings().getLocation(location);
        if (settings == null || !settings.enabled() || settings.position() != Settings.Position.BEFORE_NAME) return null;

        Component badges = service.renderActive(player.getUniqueId());
        if (badges == null) return null;
        return Component.textOfChildren(badges, settings.separator());
    }

    @Override
    public Component getSuffix(Player player, NameLocation location) {
        Settings.LocationSettings settings = plugin.getSettings().getLocation(location);
        if (settings == null || !settings.enabled() || settings.position() != Settings.Position.AFTER_NAME) return null;

        Component badges = service.renderActive(player.getUniqueId());
        if (badges == null) return null;
        return Component.textOfChildren(settings.separator(), badges);
    }

}
