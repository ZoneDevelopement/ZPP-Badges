package dev.nandi0813.zppbadges.config;

import dev.nandi0813.api.Enum.NameLocation;
import dev.nandi0813.zppbadges.ZPPBadges;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public final class Settings {

    private static final Material DEFAULT_HUB_ICON_MATERIAL = Material.NAME_TAG;
    private static final Material DEFAULT_LOCKED_MATERIAL = Material.GRAY_DYE;
    private static final Material DEFAULT_FILLER_MATERIAL = Material.BLACK_STAINED_GLASS_PANE;

    private final int maxActive;
    private final int maxPermissionLimit;
    private final int checkIntervalSeconds;
    private final String unlockSound;
    private final Component badgeSeparator;
    private final Map<NameLocation, LocationSettings> locations = new EnumMap<>(NameLocation.class);

    private final String guiTitle;
    private final Material hubIconMaterial;
    private final String hubIconName;
    private final List<String> hubIconLore;
    private final Material lockedMaterial;
    private final Material fillerMaterial;

    public Settings(ZPPBadges plugin) {
        var config = plugin.getConfig();
        Logger logger = plugin.getLogger();

        this.maxActive = config.getInt("MAX-ACTIVE", 3);
        this.maxPermissionLimit = config.getInt("MAX-PERMISSION-LIMIT", 20);
        this.checkIntervalSeconds = config.getInt("CHECK-INTERVAL-SECONDS", 60);
        this.unlockSound = config.getString("UNLOCK-SOUND", "");
        this.badgeSeparator = MiniMessage.miniMessage().deserialize(config.getString("BADGE-SEPARATOR", ""));

        for (NameLocation location : NameLocation.values()) {
            ConfigurationSection section = config.getConfigurationSection("LOCATIONS." + location.name());
            if (section == null) {
                locations.put(location, new LocationSettings(false, Position.AFTER_NAME, Component.empty()));
                continue;
            }

            Position position = Position.AFTER_NAME;
            String rawPosition = section.getString("POSITION", "AFTER_NAME");
            try {
                position = Position.valueOf(rawPosition.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid POSITION '" + rawPosition + "' for LOCATIONS." + location.name() + ", using AFTER_NAME.");
            }

            Component separator = MiniMessage.miniMessage().deserialize(section.getString("SEPARATOR", " "));
            locations.put(location, new LocationSettings(section.getBoolean("ENABLED", false), position, separator));
        }

        this.guiTitle = config.getString("GUI.TITLE", "<dark_gray>Badges <gray>(<page>/<pages>)");
        this.hubIconMaterial = parseMaterial(logger, "GUI.HUB-ICON.MATERIAL", config.getString("GUI.HUB-ICON.MATERIAL"), DEFAULT_HUB_ICON_MATERIAL);
        this.hubIconName = config.getString("GUI.HUB-ICON.NAME", "<gold><b>Badges");
        this.hubIconLore = Collections.unmodifiableList(config.getStringList("GUI.HUB-ICON.LORE"));
        this.lockedMaterial = parseMaterial(logger, "GUI.LOCKED-MATERIAL", config.getString("GUI.LOCKED-MATERIAL"), DEFAULT_LOCKED_MATERIAL);
        this.fillerMaterial = parseMaterial(logger, "GUI.FILLER-MATERIAL", config.getString("GUI.FILLER-MATERIAL"), DEFAULT_FILLER_MATERIAL);
    }

    private static Material parseMaterial(Logger logger, String path, String raw, Material fallback) {
        if (raw == null) return fallback;

        Material material = Material.matchMaterial(raw);
        if (material == null) {
            logger.warning("Invalid material '" + raw + "' for " + path + ", using " + fallback.name() + ".");
            return fallback;
        }
        return material;
    }

    public int getMaxActive() {
        return maxActive;
    }

    public int getMaxPermissionLimit() {
        return maxPermissionLimit;
    }

    public int getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }

    public String getUnlockSound() {
        return unlockSound;
    }

    public Component getBadgeSeparator() {
        return badgeSeparator;
    }

    public LocationSettings getLocation(NameLocation location) {
        return locations.get(location);
    }

    public String getGuiTitle() {
        return guiTitle;
    }

    public Material getHubIconMaterial() {
        return hubIconMaterial;
    }

    public String getHubIconName() {
        return hubIconName;
    }

    public List<String> getHubIconLore() {
        return hubIconLore;
    }

    public Material getLockedMaterial() {
        return lockedMaterial;
    }

    public Material getFillerMaterial() {
        return fillerMaterial;
    }

    public record LocationSettings(boolean enabled, Position position, Component separator) {
    }

    public enum Position {
        BEFORE_NAME,
        AFTER_NAME
    }

}
