package dev.nandi0813.zppbadges.gui;

import dev.nandi0813.api.Interface.CosmeticCategory;
import dev.nandi0813.zppbadges.ZPPBadges;
import dev.nandi0813.zppbadges.config.Settings;
import dev.nandi0813.zppbadges.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class BadgeCategory implements CosmeticCategory {

    private final ZPPBadges plugin;

    public BadgeCategory(ZPPBadges plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "zppbadges";
    }

    @Override
    public ItemStack getIcon(Player player) {
        Settings settings = plugin.getSettings();
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());

        long unlocked = data == null ? 0 : plugin.getBadgeManager().getBadges().stream()
                .filter(badge -> data.isUnlocked(badge.getId()))
                .count();
        int active = data == null ? 0 : data.getActive().size();

        TagResolver[] placeholders = {
                Placeholder.unparsed("unlocked", String.valueOf(unlocked)),
                Placeholder.unparsed("total", String.valueOf(plugin.getBadgeManager().size())),
                Placeholder.unparsed("active", String.valueOf(active)),
                Placeholder.unparsed("max", String.valueOf(plugin.getBadgeService().getMaxActive(player)))
        };

        Component name = MiniMessage.miniMessage().deserialize(settings.getHubIconName(), placeholders);
        List<Component> lore = settings.getHubIconLore().stream()
                .map(line -> MiniMessage.miniMessage().deserialize(line, placeholders))
                .toList();
        return BadgeMenu.item(settings.getHubIconMaterial(), name, lore);
    }

    @Override
    public void onClick(Player player) {
        if (plugin.getDataManager().get(player.getUniqueId()) != null) {
            new BadgeMenu(plugin, player, 0).open();
        }
    }

}
