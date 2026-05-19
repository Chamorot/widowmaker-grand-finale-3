package com.widowmaker.plugin.storage;

import com.widowmaker.plugin.WidowmakerPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.UUID;

/**
 * Persists the one-time claim of the Widowmaker to config.yml.
 * Once claimed, the owner UUID and name are locked forever.
 */
public class OwnerStorage {

    private static final String KEY_CLAIMED = "claimed";
    private static final String KEY_OWNER_UUID = "owner-uuid";
    private static final String KEY_OWNER_NAME = "owner-name";

    private final WidowmakerPlugin plugin;

    public OwnerStorage(WidowmakerPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isClaimed() {
        return plugin.getConfig().getBoolean(KEY_CLAIMED, false);
    }

    public void setClaimed(UUID ownerUuid, String ownerName) {
        FileConfiguration cfg = plugin.getConfig();
        cfg.set(KEY_CLAIMED, true);
        cfg.set(KEY_OWNER_UUID, ownerUuid.toString());
        cfg.set(KEY_OWNER_NAME, ownerName);
        plugin.saveConfig();
    }

    public UUID getOwnerUuid() {
        String raw = plugin.getConfig().getString(KEY_OWNER_UUID);
        if (raw == null) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String getOwnerName() {
        return plugin.getConfig().getString(KEY_OWNER_NAME, "Unknown");
    }
}
