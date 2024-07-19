package net.nomindev;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class GUIManager {
    private final EnderChestExpansion plugin;

    public GUIManager(EnderChestExpansion plugin) {
        this.plugin = plugin;
    }

    public Inventory createMainGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_PURPLE + "Ender Chest Expansion");

        int unlockedChests = plugin.getEnderChestManager().getUnlockedChestCount(player);
        double price = 25000 * Math.pow(1.25, unlockedChests - 1);

        for (int i = 0; i < 27; i++) {
            if (i < unlockedChests) {
                gui.setItem(i, createGUIItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "Ender Chest " + (i + 1), "Click to open"));
            } else if (i == unlockedChests) {
                gui.setItem(i, createGUIItem(Material.YELLOW_STAINED_GLASS_PANE, ChatColor.YELLOW + "Buy New Ender Chest",
                        "Click to purchase",
                        ChatColor.GOLD + "Price: $" + String.format("%.2f", price)));
            } else {
                gui.setItem(i, createGUIItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Locked", "Purchase previous chests to unlock"));
            }
        }

        return gui;
    }

    public Inventory createEnderChestGUI(Player player, int index) {
        Inventory enderChest = plugin.getEnderChestManager().getEnderChest(player, index);
        if (enderChest == null) {
            return null;
        }

        return enderChest;
    }

    public Inventory createPurchaseConfirmationGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, ChatColor.DARK_PURPLE + "Confirm Purchase");

        int unlockedChests = plugin.getEnderChestManager().getUnlockedChestCount(player);
        double price = 25000 * Math.pow(1.25, unlockedChests - 1);

        gui.setItem(3, createGUIItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "Confirm Purchase",
                "Click to buy a new Ender Chest",
                ChatColor.GOLD + "Price: $" + String.format("%.2f", price)));
        gui.setItem(5, createGUIItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Cancel", "Click to cancel the purchase"));

        return gui;
    }

    private ItemStack createGUIItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }
}