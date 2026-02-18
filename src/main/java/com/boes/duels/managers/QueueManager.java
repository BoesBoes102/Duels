package com.boes.duels.managers;

import com.boes.duels.Duels;
import com.boes.duels.data.DuelQueue;
import com.boes.duels.data.Kit;
import com.boes.duels.data.DuelMap;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QueueManager {
    private final Duels plugin;
    private final Map<String, DuelQueue> queues;
    private final Map<String, List<UUID>> playersInQueue;
    private final File queuesFile;
    private final Gson gson;

    public QueueManager(Duels plugin) {
        this.plugin = plugin;
        this.queues = new HashMap<>();
        this.playersInQueue = new ConcurrentHashMap<>();
        this.queuesFile = new File(plugin.getDataFolder(), "queues.json");
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
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

    public void createQueue(String name, String kitName, String mapName, ItemStack displayItem) {
        DuelQueue queue = new DuelQueue(name, kitName, mapName, displayItem);
        queues.put(name.toLowerCase(), queue);
        saveQueues();
    }

    public void deleteQueue(String name) {
        queues.remove(name.toLowerCase());
        playersInQueue.remove(name.toLowerCase());
        saveQueues();
    }

    public DuelQueue getQueue(String name) {
        return queues.get(name.toLowerCase());
    }

    public Collection<DuelQueue> getAllQueues() {
        return queues.values();
    }

    public void joinQueue(Player player, String queueName) {
        String lowerName = queueName.toLowerCase();
        if (!queues.containsKey(lowerName)) {
            player.sendMessage(ChatColor.RED + "Queue not found!");
            return;
        }

        if (isPlayerInAnyQueue(player)) {
            player.sendMessage(ChatColor.RED + "You are already in a queue!");
            return;
        }

        List<UUID> players = playersInQueue.computeIfAbsent(lowerName, k -> new ArrayList<>());
        synchronized (players) {
            players.add(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Joined queue: " + queues.get(lowerName).name());
            checkAndMatch(lowerName);
        }
    }

    public void leaveQueue(Player player) {
        for (Map.Entry<String, List<UUID>> entry : playersInQueue.entrySet()) {
            if (entry.getValue().remove(player.getUniqueId())) {
                player.sendMessage(ChatColor.YELLOW + "Left queue: " + queues.get(entry.getKey()).name());
                return;
            }
        }
    }

    public boolean isPlayerInAnyQueue(Player player) {
        for (List<UUID> players : playersInQueue.values()) {
            if (players.contains(player.getUniqueId())) return true;
        }
        return false;
    }

    private void checkAndMatch(String queueName) {
        List<UUID> players = playersInQueue.get(queueName);
        if (players == null || players.size() < 2) return;

        synchronized (players) {
            if (players.size() >= 2) {
                UUID uuid1 = players.remove(0);
                UUID uuid2 = players.remove(0);

                Player p1 = plugin.getServer().getPlayer(uuid1);
                Player p2 = plugin.getServer().getPlayer(uuid2);

                if (p1 == null || !p1.isOnline()) {
                    if (p2 != null && p2.isOnline()) players.add(0, uuid2);
                    return;
                }
                if (p2 == null || !p2.isOnline()) {
                    if (p1 != null && p1.isOnline()) players.add(0, uuid1);
                    return;
                }

                DuelQueue queue = queues.get(queueName);
                Kit kit = plugin.getKitManager().getKit(queue.kitName());
                DuelMap map = plugin.getMapManager().getMap(queue.mapName());

                if (kit == null || map == null) {
                    p1.sendMessage(ChatColor.RED + "Queue error: kit or map not found.");
                    p2.sendMessage(ChatColor.RED + "Queue error: kit or map not found.");
                    return;
                }

                if (map.isSpawnsSet()) {
                    p1.sendMessage(ChatColor.RED + "Queue error: map spawns are not set!");
                    p2.sendMessage(ChatColor.RED + "Queue error: map spawns are not set!");
                    return;
                }

                p1.sendMessage(ChatColor.GREEN + "Match found!");
                p2.sendMessage(ChatColor.GREEN + "Match found!");

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getDuelManager().acceptDuel(new com.boes.duels.data.DuelRequest(p1, p2, kit, map, 0));
                });
            }
        }
    }

    public void loadQueues() {
        if (!queuesFile.exists()) return;
        try (Reader reader = new InputStreamReader(new FileInputStream(queuesFile), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, DuelQueue>>(){}.getType();
            Map<String, DuelQueue> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                queues.putAll(loaded);
                plugin.getLogger().info("Loaded " + queues.size() + " duel queues");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load queues: " + e.getMessage());
        }
    }

    public void saveQueues() {
        try {
            if (!queuesFile.getParentFile().exists()) {
                queuesFile.getParentFile().mkdirs();
            }
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(queuesFile), StandardCharsets.UTF_8)) {
                gson.toJson(queues, writer);
                writer.flush();
                plugin.getLogger().info("Saved " + queues.size() + " duel queues");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save queues: " + e.getMessage());
        }
    }
}
