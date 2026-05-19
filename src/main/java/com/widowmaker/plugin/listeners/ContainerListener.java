package com.widowmaker.plugin.listeners;

import com.widowmaker.plugin.WidowmakerPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ContainerListener implements Listener {

    private final WidowmakerPlugin plugin;

    public ContainerListener(WidowmakerPlugin plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Block placing into any external inventory
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory top = event.getView().getTopInventory();
        boolean topIsExternal = isExternalInventory(top.getType());

        // Shift-click from player inv into external container
        if (event.isShiftClick() && isWidowmaker(event.getCurrentItem())) {
            if (topIsExternal) {
                event.setCancelled(true);
                notifyPlayer(player);
                return;
            }
        }

        // Direct click placement into an external slot
        Inventory clicked = event.getClickedInventory();
        if (clicked != null && isExternalInventory(clicked.getType())) {
            if (isWidowmaker(event.getCursor()) || isWidowmaker(event.getCurrentItem())) {
                event.setCancelled(true);
                notifyPlayer(player);
                return;
            }
        }

        // Hotbar number-key swap into external container
        if (event.getClick() == ClickType.NUMBER_KEY && topIsExternal) {
            int hotbarSlot = event.getHotbarButton();
            if (hotbarSlot >= 0) {
                ItemStack hotbarItem = player.getInventory().getItem(hotbarSlot);
                if (isWidowmaker(hotbarItem)) {
                    event.setCancelled(true);
                    notifyPlayer(player);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isWidowmaker(event.getOldCursor())) return;

        Inventory top = event.getView().getTopInventory();
        if (!isExternalInventory(top.getType())) return;

        int topSize = top.getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize) {
                event.setCancelled(true);
                notifyPlayer(player);
                return;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Block item frames (regular + glow via instanceof) and armor stands
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        var entity = event.getRightClicked();

        // ItemFrame covers GlowItemFrame too (it extends ItemFrame in Paper API)
        boolean isHolder = entity instanceof ItemFrame || entity instanceof ArmorStand;
        if (!isHolder) return;

        ItemStack hand = event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND
            ? player.getInventory().getItemInMainHand()
            : player.getInventory().getItemInOffHand();

        if (isWidowmaker(hand)) {
            event.setCancelled(true);
            notifyPlayer(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        var entity = event.getRightClicked();

        boolean isHolder = entity instanceof ItemFrame || entity instanceof ArmorStand;
        if (!isHolder) return;

        ItemStack hand = event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND
            ? player.getInventory().getItemInMainHand()
            : player.getInventory().getItemInOffHand();

        if (isWidowmaker(hand)) {
            event.setCancelled(true);
            notifyPlayer(player);
        }
    }

    // -------------------------------------------------------------------------
    // Block hoppers/minecart hoppers from sucking up the Widowmaker
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHopperPickup(InventoryPickupItemEvent event) {
        if (isWidowmaker(event.getItem().getItemStack())) {
            event.setCancelled(true);
        }
    }

    // -------------------------------------------------------------------------
    // Block ALL non-owner entities (players and mobs) from picking it up
    // Prevents zombies/foxes/piglins from grabbing it and despawning with it
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityPickup(EntityPickupItemEvent event) {
        if (!isWidowmaker(event.getItem().getItemStack())) return;

        // Always block non-player entities (mobs)
        if (!(event.getEntity() instanceof Player player)) {
            event.setCancelled(true);
            return;
        }

        // Block players who aren't the owner
        var storage = plugin.getOwnerStorage();
        if (!storage.isClaimed()) return; // pre-claim, let /widowmaker handle it

        if (!player.getUniqueId().equals(storage.getOwnerUuid())) {
            event.setCancelled(true);
            player.sendActionBar(
                Component.text("This weapon does not belong to you.")
                    .color(NamedTextColor.DARK_RED)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false)
            );
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean isExternalInventory(InventoryType type) {
        return switch (type) {
            case CHEST, ENDER_CHEST, SHULKER_BOX, BARREL,
                 HOPPER, DROPPER, DISPENSER,
                 FURNACE, BLAST_FURNACE, SMOKER,
                 BREWING, ANVIL, GRINDSTONE,
                 MERCHANT, BEACON, LOOM,
                 CARTOGRAPHY, STONECUTTER, CRAFTER,
                 SMITHING, WORKBENCH -> true;
            default -> false;
        };
    }

    private boolean isWidowmaker(ItemStack item) {
        return plugin.getWidowmakerItem().isWidowmaker(item);
    }

    private void notifyPlayer(Player player) {
        player.sendActionBar(
            Component.text("The Widowmaker cannot be contained.")
                .color(NamedTextColor.DARK_RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false)
        );
    }
}
