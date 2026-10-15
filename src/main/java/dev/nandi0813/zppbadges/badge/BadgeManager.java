package dev.nandi0813.zppbadges.badge;

import dev.nandi0813.zppbadges.ZPPBadges;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BadgeManager {

    private final ZPPBadges plugin;
    private final Map<String, Badge> badges = new LinkedHashMap<>();

    public BadgeManager(ZPPBadges plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "badges.yml"));
        ConfigurationSection section = config.getConfigurationSection("BADGES");
        if (section == null) {
            plugin.getLogger().warning("No BADGES section found in badges.yml.");
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection badgeSection = section.getConfigurationSection(key);
            if (badgeSection == null) continue;

            String id = key.toLowerCase(Locale.ROOT);
            String display = badgeSection.getString("DISPLAY");
            if (display == null) {
                plugin.getLogger().warning("Badge " + id + " has no DISPLAY, skipping it.");
                continue;
            }

            String rawName = badgeSection.getString("NAME", id);
            Material icon = parseIcon(id, badgeSection.getString("ICON"));

            List<Requirement> requirements = new ArrayList<>();
            List<Map<?, ?>> requirementMaps = badgeSection.getMapList("REQUIREMENTS");
            for (Map<?, ?> requirementMap : requirementMaps) {
                Requirement requirement = Requirement.parse(requirementMap, plugin.getLogger(), id);
                if (requirement != null) requirements.add(requirement);
            }
            if (!requirementMaps.isEmpty() && requirements.isEmpty()) {
                plugin.getLogger().warning("Badge " + id + " has no valid requirements, skipping it.");
                continue;
            }

            List<Component> description = badgeSection.getStringList("DESCRIPTION").stream()
                    .map(line -> MiniMessage.miniMessage().deserialize(line))
                    .toList();

            badges.put(id, new Badge(
                    id,
                    MiniMessage.miniMessage().deserialize(display),
                    MiniMessage.miniMessage().deserialize(rawName),
                    rawName,
                    description,
                    icon,
                    requirements));
        }
    }

    private Material parseIcon(String id, String raw) {
        if (raw == null) return Material.PAPER;

        Material material = Material.matchMaterial(raw);
        if (material == null) {
            plugin.getLogger().warning("Badge " + id + " has an invalid ICON '" + raw + "', using PAPER.");
            return Material.PAPER;
        }
        return material;
    }

    public Badge get(String id) {
        return badges.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Badge> getBadges() {
        return Collections.unmodifiableCollection(badges.values());
    }

    public int size() {
        return badges.size();
    }

}
