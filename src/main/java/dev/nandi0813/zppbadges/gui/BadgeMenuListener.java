package dev.nandi0813.zppbadges.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class BadgeMenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof BadgeMenu menu)) return;

        event.setCancelled(true);
        int rawSlot = event.getRawSlot();
        if (rawSlot >= 0 && rawSlot < 54 && event.getWhoClicked().equals(menu.getViewer())) {
            menu.handleClick(rawSlot);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof BadgeMenu) {
            event.setCancelled(true);
        }
    }

}
