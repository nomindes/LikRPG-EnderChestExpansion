package net.nomindev;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class EnderChestListener implements Listener {
    private final EnderChestExpansion plugin;

    public EnderChestListener(EnderChestExpansion plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.ENDER_CHEST) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            Inventory mainGUI = plugin.getGUIManager().createMainGUI(player);
            player.openInventory(mainGUI);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedInventory == null || clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        if (event.getView().getTitle().equals(ChatColor.DARK_PURPLE + "Ender Chest Expansion")) {
            event.setCancelled(true);
            handleMainGUIClick(player, clickedItem);
        } else if (event.getView().getTitle().equals(ChatColor.DARK_PURPLE + "Confirm Purchase")) {
            event.setCancelled(true);
            handlePurchaseConfirmationClick(player, clickedItem);
        } else if (event.getView().getTitle().startsWith("Ender Chest ")) {
            // Allow normal interaction within the Ender Chest inventory
            return;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();

        if (event.getView().getTitle().startsWith("Ender Chest ")) {
            int chestIndex = Integer.parseInt(event.getView().getTitle().split(" ")[2]) - 1;
            plugin.getEnderChestManager().setEnderChestContents(player, chestIndex, inventory.getContents());
            plugin.getEnderChestManager().savePlayerData(player);
            plugin.getLogger().info("Saved Ender Chest contents for " + player.getName() + ", chest index: " + chestIndex);
        }
    }

    private void handleMainGUIClick(Player player, ItemStack clickedItem) {
        if (clickedItem.getType() == Material.LIME_STAINED_GLASS_PANE) {
            int chestIndex = Integer.parseInt(clickedItem.getItemMeta().getDisplayName().split(" ")[2]) - 1;
            Inventory enderChestGUI = plugin.getGUIManager().createEnderChestGUI(player, chestIndex);
            if (enderChestGUI != null) {
                player.openInventory(enderChestGUI);
            }
        } else if (clickedItem.getType() == Material.YELLOW_STAINED_GLASS_PANE) {
            Inventory confirmationGUI = plugin.getGUIManager().createPurchaseConfirmationGUI(player);
            player.openInventory(confirmationGUI);
        }
    }

    private void handlePurchaseConfirmationClick(Player player, ItemStack clickedItem) {
        if (clickedItem.getType() == Material.LIME_STAINED_GLASS_PANE) {
            handleChestPurchase(player);
        } else if (clickedItem.getType() == Material.RED_STAINED_GLASS_PANE) {
            player.openInventory(plugin.getGUIManager().createMainGUI(player));
        }
    }

    private void handleChestPurchase(Player player) {
        int unlockedChests = plugin.getEnderChestManager().getUnlockedChestCount(player);
        double price = 25000 * Math.pow(1.25, unlockedChests - 1);

        if (plugin.getEconomy().has(player, price)) {
            plugin.getEconomy().withdrawPlayer(player, price);
            if (plugin.getEnderChestManager().unlockNewChest(player)) {
                player.sendMessage(ChatColor.GREEN + "You have successfully purchased a new Ender Chest!");
                player.openInventory(plugin.getGUIManager().createMainGUI(player));
            } else {
                player.sendMessage(ChatColor.RED + "Failed to unlock new Ender Chest. Please contact an administrator.");
                plugin.getEconomy().depositPlayer(player, price);
            }
        } else {
            player.sendMessage(ChatColor.RED + "You don't have enough money to purchase a new Ender Chest!");
            player.sendMessage(ChatColor.RED + "Price: $" + String.format("%.2f", price));
            player.openInventory(plugin.getGUIManager().createMainGUI(player));
        }
    }
}