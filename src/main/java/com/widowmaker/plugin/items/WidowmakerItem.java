package com.widowmaker.plugin.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import com.widowmaker.plugin.WidowmakerPlugin;

import java.util.List;

public class WidowmakerItem {

    public static final String WIDOWMAKER_KEY = "widowmaker";

    private final NamespacedKey itemKey;

    public WidowmakerItem() {
        this.itemKey = new NamespacedKey(WidowmakerPlugin.getInstance(), WIDOWMAKER_KEY);
    }

    public NamespacedKey getItemKey() {
        return itemKey;
    }

    public ItemStack create() {
        ItemStack item = new ItemStack(Material.CROSSBOW);

        item.editMeta(CrossbowMeta.class, meta -> {
            // Name
            meta.displayName(
                Component.text("Widowmaker")
                    .color(NamedTextColor.DARK_RED)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false)
            );

            // Lore
            meta.lore(List.of(
                Component.empty(),
                Component.text("A crossbow that was once tainted by the curses of this world,")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("once used by the greatest archer in the nether")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("SKELEROT")
                    .color(NamedTextColor.DARK_BLUE)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("After the war between the nether and the overworld this famous crossbow")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("was purified by the arch-hero and has lost most of its former glory.")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("\u2746 Arm Decaying Hexshot")
                    .color(NamedTextColor.DARK_AQUA)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("Sneak + Offhand")
                    .color(NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("Shoots an arrow filled with poison causing the enemy to lose")
                    .color(NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("their sense of grip on their sword.")
                    .color(NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Cooldown: ")
                    .color(NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false)
                    .append(
                        Component.text("45s")
                            .color(NamedTextColor.GOLD)
                            .decoration(TextDecoration.ITALIC, false)
                    ),
                Component.empty(),
                Component.text("\"...rivers of blood, screech's of agony and despair...\"")
                    .color(NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, true)
            ));

            // Enchantments
            meta.addEnchant(Enchantment.QUICK_CHARGE, 4, true);
            meta.addEnchant(Enchantment.PIERCING, 4, true);

            // Unbreakable
            meta.setUnbreakable(true);

            // Hide enchants, unbreakable tag, and attributes from tooltip
            meta.addItemFlags(
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_ATTRIBUTES
            );

            // PDC tag to identify this item
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, (byte) 1);
        });

        return item;
    }

    public boolean isWidowmaker(ItemStack item) {
        if (item == null || item.getType() != Material.CROSSBOW) return false;
        var meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(itemKey, PersistentDataType.BYTE);
    }
}
