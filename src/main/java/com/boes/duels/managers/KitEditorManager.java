package com.boes.duels.managers;

import com.boes.duels.Duels;
import com.boes.duels.data.Kit;
import com.boes.duels.data.KitLayout;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class KitEditorManager {
    private final Duels plugin;
    private final Map<UUID, String> editingPlayers;
    private final Map<UUID, Map<String, KitLayout>> playerKitLayouts;
    private final Map<UUID, Location> playerEditorLocations;
    private final File dataFile;
    private final Gson gson;
    private final Location baseEditorLocation;
    private int nextLocationId = 0;

    public KitEditorManager(Duels plugin) {
        this.plugin = plugin;
        this.editingPlayers = new HashMap<>();
        this.playerKitLayouts = new HashMap<>();
        this.playerEditorLocations = new HashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "player_kits.json");
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .enableComplexMapKeySerialization()
                .registerTypeAdapter(Optional.class, new JsonSerializer<Optional<?>>() {
                    @Override
                    public JsonElement serialize(Optional<?> src, Type typeOfSrc, JsonSerializationContext context) {
                        if (src == null || !src.isPresent()) return JsonNull.INSTANCE;
                        Object value = src.get();
                        Type valueType = value.getClass();
                        if (typeOfSrc instanceof ParameterizedType) {
                            valueType = ((ParameterizedType) typeOfSrc).getActualTypeArguments()[0];
                        }
                        return context.serialize(value, valueType);
                    }
                })
                .registerTypeAdapter(Optional.class, new JsonDeserializer<Optional<?>>() {
                    @Override
                    public Optional<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                        if (json == null || json.isJsonNull()) return Optional.empty();
                        Type valueType = Object.class;
                        if (typeOfT instanceof ParameterizedType) {
                            valueType = ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
                        }
                        return Optional.ofNullable(context.deserialize(json, valueType));
                    }
                })
                .registerTypeHierarchyAdapter(ItemStack.class, new JsonSerializer<ItemStack>() {
                    @Override
                    public JsonElement serialize(ItemStack src, Type typeOfSrc, JsonSerializationContext context) {
                        if (src == null || src.getType().isAir()) return JsonNull.INSTANCE;
                        try {
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
                            dataOutput.writeObject(src);
                            dataOutput.close();
                            String encoded = Base64.getEncoder().encodeToString(outputStream.toByteArray());
                            JsonObject jsonObject = new JsonObject();
                            jsonObject.addProperty("data", encoded);
                            return jsonObject;
                        } catch (Exception e) {
                            return JsonNull.INSTANCE;
                        }
                    }
                })
                .registerTypeHierarchyAdapter(ItemStack.class, new JsonDeserializer<ItemStack>() {
                    @Override
                    public ItemStack deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
                        if (json == null || json.isJsonNull()) return null;
                        try {
                            String encoded = json.getAsJsonObject().get("data").getAsString();
                            byte[] data = Base64.getDecoder().decode(encoded);
                            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
                            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
                            ItemStack itemStack = (ItemStack) dataInput.readObject();
                            dataInput.close();
                            return itemStack;
                        } catch (Exception e) {
                            return null;
                        }
                    }
                })
                .create();
        
        this.baseEditorLocation = new Location(plugin.getWorldManager().getDuelsWorld(), 0.5, 101, 0.5);
    }

    public void startEditing(Player player, String kitName) {
        Kit kit = plugin.getKitManager().getKit(kitName);
        if (kit == null) {
            player.sendMessage(ChatColor.RED + "Kit not found!");
            return;
        }

        plugin.getPlayerDataManager().savePlayerData(player);
        editingPlayers.put(player.getUniqueId(), kitName.toLowerCase());

        Location playerLoc = baseEditorLocation.clone().add(nextLocationId * 10, 0, 0);
        nextLocationId++;
        playerEditorLocations.put(player.getUniqueId(), playerLoc);

        Location bedrockLoc = playerLoc.clone().add(0, -1, 0);
        bedrockLoc.getBlock().setType(Material.BEDROCK);
        
        player.teleport(playerLoc);

        KitLayout customLayout = getLayout(player.getUniqueId(), kitName);
        if (customLayout != null) {
            applyLayout(player, customLayout);
        } else {
            kit.applyToPlayer(player);
        }

        player.sendMessage(ChatColor.GREEN + "You are now editing the kit: " + kitName);
        player.sendMessage(ChatColor.GREEN + "Use /duel kiteditor save to save your layout.");
    }

    public void saveEditing(Player player) {
        String kitName = editingPlayers.get(player.getUniqueId());
        if (kitName == null) {
            player.sendMessage(ChatColor.RED + "You are not editing a kit!");
            return;
        }

        ItemStack[] contents = player.getInventory().getContents();
        ItemStack[] armor = player.getInventory().getArmorContents();
        ItemStack offhand = player.getInventory().getItemInOffHand();

        KitLayout layout = new KitLayout(contents, armor, offhand);
        
        Map<String, KitLayout> layouts = playerKitLayouts.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        layouts.put(kitName, layout);
        
        saveData();

        Location playerLoc = playerEditorLocations.remove(player.getUniqueId());
        if (playerLoc != null) {
            playerLoc.clone().add(0, -1, 0).getBlock().setType(Material.AIR);
        }

        editingPlayers.remove(player.getUniqueId());
        plugin.getPlayerDataManager().restorePlayerData(player);
        
        player.sendMessage(ChatColor.GREEN + "Kit layout for " + kitName + " saved successfully!");
    }

    public boolean isEditing(Player player) {
        return editingPlayers.containsKey(player.getUniqueId());
    }

    public KitLayout getLayout(UUID uuid, String kitName) {
        Map<String, KitLayout> layouts = playerKitLayouts.get(uuid);
        if (layouts == null) return null;
        return layouts.get(kitName.toLowerCase());
    }

    private void applyLayout(Player player, KitLayout layout) {
        player.getInventory().setContents(layout.contents());
        player.getInventory().setArmorContents(layout.armor());
        player.getInventory().setItemInOffHand(layout.offhand());
    }

    public void loadData() {
        if (!dataFile.exists()) return;

        try (Reader reader = new InputStreamReader(new FileInputStream(dataFile), StandardCharsets.UTF_8)) {
            Map<UUID, Map<String, KitLayout>> loadedData = gson.fromJson(reader, new TypeToken<Map<UUID, Map<String, KitLayout>>>(){}.getType());
            if (loadedData != null) {
                playerKitLayouts.putAll(loadedData);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load player kit layouts: " + e.getMessage());
        }
    }

    public void saveData() {
        try {
            if (!dataFile.getParentFile().exists()) {
                dataFile.getParentFile().mkdirs();
            }

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(dataFile), StandardCharsets.UTF_8)) {
                gson.toJson(playerKitLayouts, writer);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player kit layouts: " + e.getMessage());
        }
    }
}
