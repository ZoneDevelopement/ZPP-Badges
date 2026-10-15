package dev.nandi0813.zppbadges.data;

import dev.nandi0813.zppbadges.ZPPBadges;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public final class PlayerDataStorage {

    private final ZPPBadges plugin;
    private final File folder;

    public PlayerDataStorage(ZPPBadges plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "playerdata");
    }

    public synchronized PlayerData load(UUID uuid) {
        PlayerData data = new PlayerData(uuid);
        File file = fileOf(uuid);
        if (!file.exists()) return data;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        data.getUnlocked().addAll(config.getStringList("UNLOCKED"));
        data.getActive().addAll(config.getStringList("ACTIVE"));
        data.setEventWins(config.getInt("EVENT-WINS"));
        data.setFfaKills(config.getInt("FFA-KILLS"));
        return data;
    }

    public synchronized void save(PlayerData data) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("UNLOCKED", new ArrayList<>(data.getUnlocked()));
        config.set("ACTIVE", new ArrayList<>(data.getActive()));
        config.set("EVENT-WINS", data.getEventWins());
        config.set("FFA-KILLS", data.getFfaKills());

        try {
            config.save(fileOf(data.getUuid()));
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save the badge data of " + data.getUuid() + ": " + e.getMessage());
        }
    }

    private File fileOf(UUID uuid) {
        return new File(folder, uuid + ".yml");
    }

}
