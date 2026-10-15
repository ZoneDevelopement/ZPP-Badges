package dev.nandi0813.zppbadges;

import dev.nandi0813.api.ZonePracticeApi;
import dev.nandi0813.zppbadges.badge.BadgeDecorator;
import dev.nandi0813.zppbadges.badge.BadgeManager;
import dev.nandi0813.zppbadges.badge.BadgeService;
import dev.nandi0813.zppbadges.command.BadgesCommand;
import dev.nandi0813.zppbadges.config.Messages;
import dev.nandi0813.zppbadges.config.Settings;
import dev.nandi0813.zppbadges.data.PlayerDataManager;
import dev.nandi0813.zppbadges.data.PlayerDataStorage;
import dev.nandi0813.zppbadges.gui.BadgeCategory;
import dev.nandi0813.zppbadges.gui.BadgeMenu;
import dev.nandi0813.zppbadges.gui.BadgeMenuListener;
import dev.nandi0813.zppbadges.listener.BadgeListener;
import dev.nandi0813.zppbadges.papi.BadgesExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;

public final class ZPPBadges extends JavaPlugin {

    private static ZPPBadges instance;

    private volatile Settings settings;
    private volatile Messages messages;
    private volatile BadgeManager badgeManager;
    private PlayerDataStorage storage;
    private PlayerDataManager dataManager;
    private BadgeService badgeService;
    private BadgeDecorator decorator;
    private BukkitTask checkTask;

    public static ZPPBadges getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        if (!ZonePracticeApi.isAvailable() || runtimeApiVersion() < 4) {
            getLogger().severe("ZonePractice Pro 2.5.0+ is required.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        if (!new File(getDataFolder(), "badges.yml").exists()) saveResource("badges.yml", false);
        if (!new File(getDataFolder(), "messages.yml").exists()) saveResource("messages.yml", false);

        reloadAll();

        storage = new PlayerDataStorage(this);
        dataManager = new PlayerDataManager(this, storage);
        badgeService = new BadgeService(this);
        decorator = new BadgeDecorator(this, badgeService);
        ZonePracticeApi.getInstance().getNameDecorationRegistry().register(this, decorator);
        getServer().getPluginManager().registerEvents(new BadgeListener(this), this);
        getServer().getPluginManager().registerEvents(new BadgeMenuListener(), this);
        ZonePracticeApi.getInstance().getCosmeticsRegistry().register(this, new BadgeCategory(this));

        BadgesCommand badgesCommand = new BadgesCommand(this);
        getCommand("badges").setExecutor(badgesCommand);
        getCommand("badges").setTabCompleter(badgesCommand);

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) new BadgesExpansion(this).register();
        startCheckTask();

        for (Player player : Bukkit.getOnlinePlayers()) {
            dataManager.load(player, data -> {
                badgeService.trimActive(player);
                badgeService.checkUnlocks(player);
                badgeService.refreshName(player);
            });
        }
    }

    @Override
    public void onDisable() {
        if (checkTask != null) checkTask.cancel();
        if (dataManager != null) dataManager.saveAllSync();
        if (ZonePracticeApi.isAvailable()) {
            ZonePracticeApi.getInstance().getNameDecorationRegistry().unregister("zppbadges");
            ZonePracticeApi.getInstance().getCosmeticsRegistry().unregister("zppbadges");
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof BadgeMenu) player.closeInventory();
        }
    }

    private static int runtimeApiVersion() {
        try {
            return ZonePracticeApi.class.getField("API_VERSION").getInt(null);
        } catch (ReflectiveOperationException e) {
            return 0;
        }
    }

    public void reloadAll() {
        reloadConfig();
        this.settings = new Settings(this);
        this.messages = new Messages(this);
        this.badgeManager = new BadgeManager(this);
    }

    public void reload() {
        reloadAll();
        startCheckTask();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (dataManager.get(player.getUniqueId()) == null) continue;
            badgeService.trimActive(player);
            badgeService.refreshName(player);
        }
    }

    private void startCheckTask() {
        if (checkTask != null) checkTask.cancel();

        long period = Math.max(5, settings.getCheckIntervalSeconds()) * 20L;
        checkTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                badgeService.checkUnlocks(player);
            }
            dataManager.saveDirtyAsync();
        }, period, period);
    }

    public Settings getSettings() {
        return settings;
    }

    public Messages getMessages() {
        return messages;
    }

    public BadgeManager getBadgeManager() {
        return badgeManager;
    }

    public PlayerDataManager getDataManager() {
        return dataManager;
    }

    public BadgeService getBadgeService() {
        return badgeService;
    }

}
