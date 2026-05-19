package com.widowmaker.plugin;

import com.widowmaker.plugin.commands.OwnerCommand;
import com.widowmaker.plugin.commands.WidowmakerCommand;
import com.widowmaker.plugin.items.WidowmakerItem;
import com.widowmaker.plugin.listeners.ContainerListener;
import com.widowmaker.plugin.listeners.HexshotListener;
import com.widowmaker.plugin.storage.OwnerStorage;
import org.bukkit.plugin.java.JavaPlugin;

public class WidowmakerPlugin extends JavaPlugin {

    private static WidowmakerPlugin instance;
    private WidowmakerItem widowmakerItem;
    private OwnerStorage ownerStorage;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        widowmakerItem = new WidowmakerItem();
        ownerStorage = new OwnerStorage(this);

        // Listeners
        getServer().getPluginManager().registerEvents(new HexshotListener(this), this);
        getServer().getPluginManager().registerEvents(new ContainerListener(this), this);

        // Commands
        var cmd = getCommand("widowmaker");
        if (cmd != null) {
            var handler = new WidowmakerCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }

        var ownerCmd = getCommand("owner");
        if (ownerCmd != null) {
            var handler = new OwnerCommand(this);
            ownerCmd.setExecutor(handler);
            ownerCmd.setTabCompleter(handler);
        }

        getLogger().info("Widowmaker has awakened. The curse of Skelerot lives on.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Widowmaker has gone silent.");
    }

    public static WidowmakerPlugin getInstance() {
        return instance;
    }

    public WidowmakerItem getWidowmakerItem() {
        return widowmakerItem;
    }

    public OwnerStorage getOwnerStorage() {
        return ownerStorage;
    }
}
