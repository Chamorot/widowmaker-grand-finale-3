package com.widowmaker.plugin.commands;

import com.widowmaker.plugin.WidowmakerPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class WidowmakerCommand implements CommandExecutor, TabCompleter {

    private final WidowmakerPlugin plugin;

    public WidowmakerCommand(WidowmakerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }

        // Already claimed by someone else?
        if (plugin.getOwnerStorage().isClaimed()) {
            String ownerName = plugin.getOwnerStorage().getOwnerName();
            player.sendMessage(
                Component.text("The Widowmaker has already been claimed by ")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false)
                    .append(
                        Component.text(ownerName)
                            .color(NamedTextColor.DARK_RED)
                            .decoration(TextDecoration.BOLD, true)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                    .append(
                        Component.text(". It can never be claimed again.")
                            .color(NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false)
                    )
            );
            return true;
        }

        // Claim it
        plugin.getOwnerStorage().setClaimed(player.getUniqueId(), player.getName());

        var item = plugin.getWidowmakerItem().create();
        player.getInventory().addItem(item);

        // Broadcast to the server
        plugin.getServer().broadcast(
            Component.text("\u26A1 ")
                .color(NamedTextColor.GOLD)
                .append(
                    Component.text(player.getName())
                        .color(NamedTextColor.DARK_RED)
                        .decoration(TextDecoration.BOLD, true)
                )
                .append(
                    Component.text(" has claimed the ")
                        .color(NamedTextColor.GRAY)
                )
                .append(
                    Component.text("Widowmaker")
                        .color(NamedTextColor.DARK_RED)
                        .decoration(TextDecoration.BOLD, true)
                )
                .append(
                    Component.text(". The curse of Skelerot endures.")
                        .color(NamedTextColor.GRAY)
                )
        );

        player.sendMessage(
            Component.text("The Widowmaker is now yours — and yours alone.")
                .color(NamedTextColor.DARK_RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false)
        );
        player.sendMessage(
            Component.text("Sneak + Offhand to unleash the Arm Decaying Hexshot.")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
        );

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        return List.of();
    }
}
