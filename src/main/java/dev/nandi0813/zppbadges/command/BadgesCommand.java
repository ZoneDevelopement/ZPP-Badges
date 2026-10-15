package dev.nandi0813.zppbadges.command;

import dev.nandi0813.zppbadges.ZPPBadges;
import dev.nandi0813.zppbadges.badge.Badge;
import dev.nandi0813.zppbadges.data.PlayerData;
import dev.nandi0813.zppbadges.gui.BadgeMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public final class BadgesCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ADMIN_SUBCOMMANDS = List.of("give", "take", "reset", "list", "reload");
    private static final List<String> PLAYER_SUBCOMMANDS = List.of("give", "take", "reset", "list");
    private static final List<String> BADGE_SUBCOMMANDS = List.of("give", "take");

    private final ZPPBadges plugin;

    public BadgesCommand(ZPPBadges plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return openMenu(sender);

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (!ADMIN_SUBCOMMANDS.contains(subcommand)) {
            if (sender.hasPermission("zppbadges.admin")) {
                plugin.getMessages().sendList(sender, "ADMIN.USAGE");
                return true;
            }
            return openMenu(sender);
        }

        if (!sender.hasPermission("zppbadges.admin")) {
            plugin.getMessages().send(sender, "NO-PERMISSION");
            return true;
        }
        if (args.length != expectedArgs(subcommand)) {
            plugin.getMessages().sendList(sender, "ADMIN.USAGE");
            return true;
        }

        switch (subcommand) {
            case "give" -> give(sender, args[1], args[2]);
            case "take" -> take(sender, args[1], args[2]);
            case "reset" -> reset(sender, args[1]);
            case "list" -> list(sender, args[1]);
            case "reload" -> {
                plugin.reload();
                plugin.getMessages().send(sender, "ADMIN.RELOADED");
            }
            default -> plugin.getMessages().sendList(sender, "ADMIN.USAGE");
        }
        return true;
    }

    private boolean openMenu(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.getMessages().send(sender, "PLAYER-ONLY");
            return true;
        }
        if (!player.hasPermission("zppbadges.use")) {
            plugin.getMessages().send(sender, "NO-PERMISSION");
            return true;
        }

        if (plugin.getDataManager().get(player.getUniqueId()) != null) {
            new BadgeMenu(plugin, player, 0).open();
        }
        return true;
    }

    private void give(CommandSender sender, String playerName, String badgeId) {
        Player target = findTarget(sender, playerName);
        if (target == null) return;
        Badge badge = findBadge(sender, badgeId);
        if (badge == null) return;

        if (plugin.getBadgeService().give(target, badge)) {
            plugin.getMessages().send(sender, "ADMIN.GIVEN",
                    Placeholder.unparsed("player", target.getName()),
                    Placeholder.component("badge_name", badge.getName()));
        } else {
            plugin.getMessages().send(sender, "ADMIN.ALREADY-UNLOCKED", Placeholder.unparsed("player", target.getName()));
        }
    }

    private void take(CommandSender sender, String playerName, String badgeId) {
        Player target = findTarget(sender, playerName);
        if (target == null) return;
        Badge badge = findBadge(sender, badgeId);
        if (badge == null) return;

        if (plugin.getBadgeService().take(target, badge)) {
            plugin.getMessages().send(sender, "ADMIN.TAKEN",
                    Placeholder.unparsed("player", target.getName()),
                    Placeholder.component("badge_name", badge.getName()));
        } else {
            plugin.getMessages().send(sender, "ADMIN.NOT-UNLOCKED", Placeholder.unparsed("player", target.getName()));
        }
    }

    private void reset(CommandSender sender, String playerName) {
        Player target = findTarget(sender, playerName);
        if (target == null) return;

        plugin.getBadgeService().reset(target);
        plugin.getMessages().send(sender, "ADMIN.RESET", Placeholder.unparsed("player", target.getName()));
    }

    private void list(CommandSender sender, String playerName) {
        Player target = findTarget(sender, playerName);
        if (target == null) return;

        PlayerData data = plugin.getDataManager().get(target.getUniqueId());
        List<Component> unlocked = plugin.getBadgeManager().getBadges().stream()
                .filter(badge -> data.isUnlocked(badge.getId()))
                .<Component>map(badge -> Component.textOfChildren(badge.getDisplay(), Component.space(), badge.getName()))
                .toList();
        Component badges = unlocked.isEmpty()
                ? Component.text("-")
                : Component.join(JoinConfiguration.separator(Component.text(", ")), unlocked);

        plugin.getMessages().send(sender, "ADMIN.LIST",
                Placeholder.unparsed("player", target.getName()),
                Placeholder.unparsed("count", String.valueOf(unlocked.size())),
                Placeholder.unparsed("total", String.valueOf(plugin.getBadgeManager().size())),
                Placeholder.component("badges", badges));
    }

    private Player findTarget(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null || plugin.getDataManager().get(target.getUniqueId()) == null) {
            plugin.getMessages().send(sender, "PLAYER-NOT-FOUND", Placeholder.unparsed("player", playerName));
            return null;
        }
        return target;
    }

    private Badge findBadge(CommandSender sender, String badgeId) {
        Badge badge = plugin.getBadgeManager().get(badgeId);
        if (badge == null) {
            plugin.getMessages().send(sender, "BADGE-NOT-FOUND", Placeholder.unparsed("badge", badgeId));
        }
        return badge;
    }

    private static int expectedArgs(String subcommand) {
        return switch (subcommand) {
            case "give", "take" -> 3;
            case "reset", "list" -> 2;
            default -> 1;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("zppbadges.admin")) return List.of();

        if (args.length == 1) return filter(ADMIN_SUBCOMMANDS, args[0]);

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && PLAYER_SUBCOMMANDS.contains(subcommand)) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 3 && BADGE_SUBCOMMANDS.contains(subcommand)) {
            return filter(plugin.getBadgeManager().getBadges().stream().map(Badge::getId).toList(), args[2]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lowerPrefix))
                .toList();
    }

}
