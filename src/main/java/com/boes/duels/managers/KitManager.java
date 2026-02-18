package com.boes.duels.managers;

import com.boes.duels.Duels;
import com.boes.duels.data.Kit;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class KitManager {
    private final Duels plugin;
    private final Map<String, Kit> kits;
    private final File dataFile;
    private final Gson gson;

    public KitManager(Duels plugin) {
        this.plugin = plugin;
        this.kits = new HashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "kits.json");
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
    }

    public void createKit(Player player, String name, Material icon) {
        if (kits.size() >= 25) {
            player.sendMessage(ChatColor.RED + "Maximum number of kits (25) reached!");
            return;
        }
        ItemStack[] contents = player.getInventory().getContents();
        ItemStack[] armor = player.getInventory().getArmorContents();
        ItemStack offhand = player.getInventory().getItemInOffHand();

        Kit kit = new Kit(name, icon, contents, armor, offhand);
        kits.put(name.toLowerCase(), kit);
        saveKits();

        player.sendMessage(ChatColor.GREEN + "Kit '" + name + "' created successfully!");
    }

    public Kit getKit(String name) {
        return kits.get(name.toLowerCase());
    }

    public Collection<Kit> getAllKits() {
        return kits.values();
    }

    public void loadKits() {
        if (!dataFile.exists()) {
            return;
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(dataFile), StandardCharsets.UTF_8)) {
            Map<String, Kit> loadedKits = gson.fromJson(reader, new TypeToken<Map<String, Kit>>(){}.getType());
            if (loadedKits != null) {
                kits.putAll(loadedKits);
            }
            plugin.getLogger().info("Loaded " + kits.size() + " kits");
        } catch (JsonSyntaxException e) {
            plugin.getLogger().severe("Failed to load kits: JSON file is corrupted. Deleting and starting fresh.");
            plugin.getLogger().severe("Error: " + e.getMessage());
            dataFile.delete();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load kits: " + e.getMessage());
        }
    }

    public void saveKits() {
        try {
            if (!dataFile.getParentFile().exists()) {
                dataFile.getParentFile().mkdirs();
            }

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(dataFile), StandardCharsets.UTF_8)) {
                gson.toJson(kits, writer);
                writer.flush();
                plugin.getLogger().info("Saved " + kits.size() + " kits");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save kits: " + e.getMessage());
        }
    }

    public void deleteKit(String name) {
        kits.remove(name.toLowerCase());
        saveKits();
    }
}