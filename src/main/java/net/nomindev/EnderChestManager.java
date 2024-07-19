package net.nomindev;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnderChestManager {
    private final EnderChestExpansion plugin;
    private final Map<UUID, PlayerEnderChestData> playerData;
    private final Gson gson;
    private final File dataFolder;

    public EnderChestManager(EnderChestExpansion plugin) {
        this.plugin = plugin;
        this.playerData = new HashMap<>();
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(ItemStack[].class, new ItemStackArrayAdapter())
                .create();
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public void loadAllPlayerData() {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try (FileReader reader = new FileReader(file)) {
                    UUID uuid = UUID.fromString(file.getName().replace(".json", ""));
                    PlayerEnderChestData data = gson.fromJson(reader, PlayerEnderChestData.class);
                    playerData.put(uuid, data);
                    plugin.getLogger().info("Loaded data for player UUID: " + uuid);
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to load data from file: " + file.getName());
                    e.printStackTrace();
                }
            }
        }
    }

    public void loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        File playerFile = new File(dataFolder, uuid.toString() + ".json");
        if (playerFile.exists()) {
            try (FileReader reader = new FileReader(playerFile)) {
                PlayerEnderChestData data = gson.fromJson(reader, PlayerEnderChestData.class);
                playerData.put(uuid, data);
                plugin.getLogger().info("Loaded data for player " + player.getName());
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load data for player " + player.getName());
                e.printStackTrace();
            }
        } else {
            playerData.put(uuid, new PlayerEnderChestData());
            plugin.getLogger().info("Created new data for player " + player.getName());
        }
    }

    public void savePlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        File playerFile = new File(dataFolder, uuid.toString() + ".json");
        try {
            plugin.getLogger().info("Attempting to save data for player: " + player.getName());
            plugin.getLogger().info("File path: " + playerFile.getAbsolutePath());

            PlayerEnderChestData data = playerData.get(uuid);
            plugin.getLogger().info("Player data: " + data);

            String json = gson.toJson(data);
            plugin.getLogger().info("JSON to save: " + json);

            FileWriter writer = new FileWriter(playerFile);
            writer.write(json);
            writer.close();

            plugin.getLogger().info("Successfully saved data for player: " + player.getName());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save data for player " + player.getName());
            e.printStackTrace();
        }
    }

    public void saveAllData() {
        for (Map.Entry<UUID, PlayerEnderChestData> entry : playerData.entrySet()) {
            File playerFile = new File(dataFolder, entry.getKey().toString() + ".json");
            try (FileWriter writer = new FileWriter(playerFile)) {
                gson.toJson(entry.getValue(), writer);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save data for player " + entry.getKey());
                e.printStackTrace();
            }
        }
    }

    public int getUnlockedChestCount(Player player) {
        UUID uuid = player.getUniqueId();
        if (!playerData.containsKey(uuid)) {
            playerData.put(uuid, new PlayerEnderChestData());
        }
        return playerData.get(uuid).getUnlockedChestCount();
    }

    public boolean unlockNewChest(Player player) {
        PlayerEnderChestData data = playerData.get(player.getUniqueId());
        if (data.unlockNewChest()) {
            savePlayerData(player);
            return true;
        }
        return false;
    }

    public Inventory getEnderChest(Player player, int index) {
        return playerData.get(player.getUniqueId()).getEnderChest(index);
    }

    public void setEnderChestContents(Player player, int index, ItemStack[] contents) {
        playerData.get(player.getUniqueId()).setEnderChestContents(index, contents);
        savePlayerData(player);
    }

    private static class PlayerEnderChestData {
        private int unlockedChestCount;
        private Map<Integer, ItemStack[]> enderChests;

        public PlayerEnderChestData() {
            this.unlockedChestCount = 1;
            this.enderChests = new HashMap<>();
        }

        public int getUnlockedChestCount() {
            return unlockedChestCount;
        }

        public boolean unlockNewChest() {
            unlockedChestCount++;
            return true;
        }

        public Inventory getEnderChest(int index) {
            if (index >= unlockedChestCount) {
                return null;
            }
            Inventory inventory = Bukkit.createInventory(null, 27, "Ender Chest " + (index + 1));
            ItemStack[] contents = enderChests.get(index);
            if (contents != null) {
                inventory.setContents(contents);
            }
            return inventory;
        }

        public void setEnderChestContents(int index, ItemStack[] contents) {
            enderChests.put(index, contents);
        }
    }

    private static class ItemStackArrayAdapter implements JsonSerializer<ItemStack[]>, JsonDeserializer<ItemStack[]> {
        @Override
        public JsonElement serialize(ItemStack[] src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(itemStackArrayToBase64(src));
        }

        @Override
        public ItemStack[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return itemStackArrayFromBase64(json.getAsString());
        }
    }

    private static String itemStackArrayToBase64(ItemStack[] items) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            dataOutput.writeInt(items.length);

            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }

            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    private static ItemStack[] itemStackArrayFromBase64(String data) throws IllegalStateException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];

            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }

            dataInput.close();
            return items;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load item stacks.", e);
        }
    }
}