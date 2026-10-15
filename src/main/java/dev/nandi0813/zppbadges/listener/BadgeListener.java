package dev.nandi0813.zppbadges.listener;

import dev.nandi0813.api.Enum.FightType;
import dev.nandi0813.api.Event.EloChangeEvent;
import dev.nandi0813.api.Event.Event.EventEndEvent;
import dev.nandi0813.api.Event.FightDeathEvent;
import dev.nandi0813.api.Event.Match.MatchEndEvent;
import dev.nandi0813.zppbadges.ZPPBadges;
import dev.nandi0813.zppbadges.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;

public final class BadgeListener implements Listener {

    private final ZPPBadges plugin;

    public BadgeListener(ZPPBadges plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getDataManager().load(player, data -> Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            plugin.getBadgeService().trimActive(player);
            plugin.getBadgeService().checkUnlocks(player);
            plugin.getBadgeService().refreshName(player);
        }, 20L));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.getDataManager().unload(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMatchEnd(MatchEndEvent event) {
        List<Player> players = new ArrayList<>(event.getMatch().getPlayers());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : players) {
                if (player.isOnline()) plugin.getBadgeService().checkUnlocks(player);
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEloChange(EloChangeEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) plugin.getBadgeService().checkUnlocks(player);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFightDeath(FightDeathEvent event) {
        if (!event.hasKiller() || event.getKiller().equals(event.getVictim())) return;

        Player killer = event.getKiller();
        if (event.getFightType() == FightType.FFA) {
            PlayerData data = plugin.getDataManager().get(killer.getUniqueId());
            if (data != null) data.incrementFfaKills();
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (killer.isOnline()) plugin.getBadgeService().checkUnlocks(killer);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEventEnd(EventEndEvent event) {
        Player winner = event.getEvent() != null ? event.getEvent().getWinner() : null;
        if (winner == null) return;

        PlayerData data = plugin.getDataManager().get(winner.getUniqueId());
        if (data != null) data.incrementEventWins();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (winner.isOnline()) plugin.getBadgeService().checkUnlocks(winner);
        });
    }

}
