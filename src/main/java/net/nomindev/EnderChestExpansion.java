package net.nomindev;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import net.milkbowl.vault.economy.Economy;

public class EnderChestExpansion extends JavaPlugin implements Listener {
    private static Economy econ = null;
    private EnderChestManager enderChestManager;
    private GUIManager guiManager;

    @Override
    public void onEnable() {
        // Setup Vault
        if (!setupEconomy()) {
            getLogger().severe("Disabled due to no Vault dependency found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize managers
        enderChestManager = new EnderChestManager(this);
        guiManager = new GUIManager(this);

        // Register events
        getServer().getPluginManager().registerEvents(new EnderChestListener(this), this);
        getServer().getPluginManager().registerEvents(this, this);

        // Register command
        getCommand("ec").setExecutor(new EnderChestCommand(this));

        // Load all player data
        enderChestManager.loadAllPlayerData();

        getLogger().info("EnderChestExpansion has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save all data
        enderChestManager.saveAllData();
        getLogger().info("EnderChestExpansion has been disabled!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        enderChestManager.loadPlayerData(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        enderChestManager.savePlayerData(event.getPlayer());
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public Economy getEconomy() {
        return econ;
    }

    public EnderChestManager getEnderChestManager() {
        return enderChestManager;
    }

    public GUIManager getGUIManager() {
        return guiManager;
    }
}