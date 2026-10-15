package dev.nandi0813.zppbadges.gui;

import dev.nandi0813.api.ZonePracticeApi;
import dev.nandi0813.zppbadges.ZPPBadges;
import dev.nandi0813.zppbadges.badge.Badge;
import dev.nandi0813.zppbadges.badge.BadgeService;
import dev.nandi0813.zppbadges.badge.Requirement;
import dev.nandi0813.zppbadges.config.Messages;
import dev.nandi0813.zppbadges.config.Settings;
import dev.nandi0813.zppbadges.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BadgeMenu implements InventoryHolder {

    private static final int SIZE = 54;
    private static final int PAGE_SIZE = 45;

    private static final int PREVIOUS_SLOT = 45;
    private static final int INFO_SLOT = 47;
    private static final int CLEAR_SLOT = 49;
    private static final int BACK_SLOT = 51;
    private static final int NEXT_SLOT = 53;

    private final ZPPBadges plugin;
    private final Player viewer;
    private final int page;
    private final Map<Integer, String> slotBadges = new HashMap<>();
    private Inventory inventory;

    public BadgeMenu(ZPPBadges plugin, Player viewer, int page) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.page = page;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getViewer() {
        return viewer;
    }

    public void open() {
        int pages = Math.max(1, (plugin.getBadgeManager().size() + PAGE_SIZE - 1) / PAGE_SIZE);
        Component title = MiniMessage.miniMessage().deserialize(plugin.getSettings().getGuiTitle(),
                Placeholder.unparsed("page", String.valueOf(page + 1)),
                Placeholder.unparsed("pages", String.valueOf(pages)));

        inventory = Bukkit.createInventory(this, SIZE, title);
        render();
        viewer.openInventory(inventory);
    }

    public void render() {
        PlayerData data = plugin.getDataManager().get(viewer.getUniqueId());
        if (data == null) {
            viewer.closeInventory();
            return;
        }

        Settings settings = plugin.getSettings();
        Messages messages = plugin.getMessages();
        BadgeService service = plugin.getBadgeService();
        List<Badge> badges = new ArrayList<>(plugin.getBadgeManager().getBadges());

        inventory.clear();
        slotBadges.clear();

        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            int index = page * PAGE_SIZE + slot;
            if (index >= badges.size()) break;

            Badge badge = badges.get(index);
            inventory.setItem(slot, badgeItem(badge, data, settings, messages));
            slotBadges.put(slot, badge.getId());
        }

        for (int slot = PREVIOUS_SLOT; slot < SIZE; slot++) {
            inventory.setItem(slot, item(settings.getFillerMaterial(), Component.empty(), List.of()));
        }

        if (page > 0) {
            inventory.setItem(PREVIOUS_SLOT, item(Material.ARROW, messages.get("GUI.PREVIOUS-PAGE"), List.of()));
        }
        inventory.setItem(INFO_SLOT, infoItem(data, messages, service));
        inventory.setItem(CLEAR_SLOT, item(Material.BARRIER, messages.get("GUI.CLEAR"), List.of()));
        inventory.setItem(BACK_SLOT, item(Material.OAK_DOOR, messages.get("GUI.BACK"), List.of()));
        if (hasNextPage()) {
            inventory.setItem(NEXT_SLOT, item(Material.ARROW, messages.get("GUI.NEXT-PAGE"), List.of()));
        }
    }

    public void handleClick(int slot) {
        String badgeId = slotBadges.get(slot);
        if (badgeId != null) {
            Badge badge = plugin.getBadgeManager().get(badgeId);
            if (badge == null) return;

            plugin.getBadgeService().toggle(viewer, badge);
            render();
            return;
        }

        switch (slot) {
            case PREVIOUS_SLOT -> {
                if (page > 0) new BadgeMenu(plugin, viewer, page - 1).open();
            }
            case NEXT_SLOT -> {
                if (hasNextPage()) new BadgeMenu(plugin, viewer, page + 1).open();
            }
            case CLEAR_SLOT -> {
                plugin.getBadgeService().clearActive(viewer);
                render();
            }
            case BACK_SLOT -> {
                viewer.closeInventory();
                ZonePracticeApi.getInstance().openCosmeticsMenu(viewer);
            }
            default -> {
            }
        }
    }

    private boolean hasNextPage() {
        return (page + 1) * PAGE_SIZE < plugin.getBadgeManager().size();
    }

    private ItemStack infoItem(PlayerData data, Messages messages, BadgeService service) {
        Component preview = service.renderActive(viewer.getUniqueId());
        return item(Material.NAME_TAG, messages.get("GUI.INFO-NAME"), messages.getList("GUI.INFO-LORE",
                Placeholder.unparsed("unlocked", String.valueOf(unlockedCount(data))),
                Placeholder.unparsed("total", String.valueOf(plugin.getBadgeManager().size())),
                Placeholder.unparsed("active", String.valueOf(data.getActive().size())),
                Placeholder.unparsed("max", String.valueOf(service.getMaxActive(viewer))),
                Placeholder.component("preview", preview == null ? Component.text("-") : preview)));
    }

    private ItemStack badgeItem(Badge badge, PlayerData data, Settings settings, Messages messages) {
        Component name = Component.textOfChildren(badge.getDisplay(), Component.space(), badge.getName());
        List<Component> lore = new ArrayList<>(badge.getDescription());

        if (data.isActive(badge.getId())) {
            lore.addAll(messages.getList("GUI.ACTIVE-LORE",
                    Placeholder.unparsed("slot", String.valueOf(data.getActive().indexOf(badge.getId()) + 1))));
            ItemStack item = item(badge.getIcon(), name, lore);
            item.editMeta(meta -> meta.setEnchantmentGlintOverride(true));
            return item;
        }

        if (data.isUnlocked(badge.getId())) {
            lore.addAll(messages.getList("GUI.UNLOCKED-LORE"));
            return item(badge.getIcon(), name, lore);
        }

        lore.addAll(messages.getList("GUI.LOCKED-LORE-HEADER"));
        if (badge.isAdminOnly()) {
            lore.addAll(messages.getList("GUI.ADMIN-ONLY-LORE"));
        } else {
            for (Requirement requirement : badge.getRequirements()) {
                lore.add(requirement.progressLine(viewer, data, messages));
            }
        }
        return item(settings.getLockedMaterial(), name, lore);
    }

    private long unlockedCount(PlayerData data) {
        return plugin.getBadgeManager().getBadges().stream()
                .filter(badge -> data.isUnlocked(badge.getId()))
                .count();
    }

    static ItemStack item(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(noItalic(name));
        meta.lore(lore.stream().map(BadgeMenu::noItalic).toList());
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    static Component noItalic(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }

}
