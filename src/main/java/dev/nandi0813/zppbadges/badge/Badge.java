package dev.nandi0813.zppbadges.badge;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.List;

public final class Badge {

    private final String id;
    private final Component display;
    private final Component name;
    private final String rawName;
    private final List<Component> description;
    private final Material icon;
    private final List<Requirement> requirements;

    public Badge(String id, Component display, Component name, String rawName, List<Component> description, Material icon, List<Requirement> requirements) {
        this.id = id;
        this.display = display;
        this.name = name;
        this.rawName = rawName;
        this.description = List.copyOf(description);
        this.icon = icon;
        this.requirements = List.copyOf(requirements);
    }

    public String getId() {
        return id;
    }

    public Component getDisplay() {
        return display;
    }

    public Component getName() {
        return name;
    }

    public String getRawName() {
        return rawName;
    }

    public List<Component> getDescription() {
        return description;
    }

    public Material getIcon() {
        return icon;
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }

    public boolean isAdminOnly() {
        return requirements.isEmpty();
    }

}
