package dev.nandi0813.zppbadges.config;

import dev.nandi0813.zppbadges.ZPPBadges;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class Messages {

    private final YamlConfiguration config;

    public Messages(ZPPBadges plugin) {
        this.config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));

        try (InputStream resource = plugin.getResource("messages.yml")) {
            if (resource != null) {
                config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(resource, StandardCharsets.UTF_8)));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not load the default messages.yml: " + e.getMessage());
        }
    }

    public Component get(String path, TagResolver... resolvers) {
        return MiniMessage.miniMessage().deserialize(raw(path), resolvers);
    }

    public List<Component> getList(String path, TagResolver... resolvers) {
        List<String> lines = config.getStringList(path);
        if (lines.isEmpty()) return List.of(get(path, resolvers));

        return lines.stream()
                .map(line -> MiniMessage.miniMessage().deserialize(line, resolvers))
                .toList();
    }

    public String raw(String path) {
        String value = config.getString(path);
        return value != null ? value : path;
    }

    public void send(CommandSender sender, String path, TagResolver... resolvers) {
        sender.sendMessage(get("PREFIX").append(get(path, resolvers)));
    }

    public void sendList(CommandSender sender, String path) {
        for (Component line : getList(path)) {
            sender.sendMessage(line);
        }
    }

}
