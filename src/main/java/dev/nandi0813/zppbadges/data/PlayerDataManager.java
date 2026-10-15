package dev.nandi0813.zppbadges.data;

import dev.nandi0813.zppbadges.ZPPBadges;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class PlayerDataManager {

    private final ZPPBadges plugin;
    private final PlayerDataStorage storage;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerData> unloading = new ConcurrentHashMap<>();

    public PlayerDataManager(ZPPBadges plugin, PlayerDataStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public PlayerData get(UUID uuid) {
        return cache.get(uuid);
    }

    public Collection<PlayerData> getAll() {
        return Collections.unmodifiableCollection(cache.values());
    }

    public void load(Player player, Consumer<PlayerData> onLoaded) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData pending = unloading.get(player.getUniqueId());
            PlayerData data = pending != null ? pending : storage.load(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                cache.put(player.getUniqueId(), data);
                onLoaded.accept(data);
            });
        });
    }

    public void unload(UUID uuid) {
        PlayerData removed = cache.remove(uuid);
        if (removed == null) return;

        unloading.put(uuid, removed);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            storage.save(removed);
            unloading.remove(uuid, removed);
        });
    }

    public void saveAsync(PlayerData data) {
        data.setDirty(false);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> storage.save(data));
    }

    public void saveDirtyAsync() {
        for (PlayerData data : cache.values()) {
            if (data.isDirty()) saveAsync(data);
        }
    }

    public void saveAllSync() {
        for (PlayerData data : cache.values()) {
            storage.save(data);
        }
    }

}
