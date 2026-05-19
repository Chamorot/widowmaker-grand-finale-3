package com.widowmaker.plugin.commands;

import com.widowmaker.plugin.WidowmakerPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class OwnerCommand implements CommandExecutor, TabCompleter {

    private final WidowmakerPlugin plugin;

    public OwnerCommand(WidowmakerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        // Must be: /owner widowmaker
        if (args.length == 0 || !args[0].equalsIgnoreCase("widowmaker")) {
            sender.sendMessage(
                Component.text("Usage: /owner widowmaker")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false)
            );
            return true;
        }

        if (!plugin.getOwnerStorage().isClaimed()) {
            sender.sendMessage(
                Component.text("The Widowmaker has not yet been claimed by anyone.")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)
            );
            return true;
        }

        String ownerName = plugin.getOwnerStorage().getOwnerName();
        sender.sendMessage(
            Component.text("\u2746 Widowmaker Owner: ")
                .color(NamedTextColor.DARK_AQUA)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false)
                .append(
                    Component.text(ownerName)
                        .color(NamedTextColor.DARK_RED)
                        .decoration(TextDecoration.BOLD, true)
                        .decoration(TextDecoration.ITALIC, false)
                )
        );

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return List.of("widowmaker");
        return List.of();
    }
}
