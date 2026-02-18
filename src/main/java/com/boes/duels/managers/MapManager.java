package com.boes.duels.managers;

import com.boes.duels.Duels;
import com.boes.duels.data.DuelMap;
import com.boes.duels.data.PregeneratedArena;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MapManager {
    private final Duels plugin;
    private final Map<String, DuelMap> maps;
    private final Map<String, List<PregeneratedArena>> pregeneratedArenas;
    private final File dataFolder;
    private final File mapsDataFile;
    private final File pregeneratedDataFile;
    private final Gson gson;

    public MapManager(Duels plugin) {
        this.plugin = plugin;
        this.maps = new HashMap<>();
        this.pregeneratedArenas = new ConcurrentHashMap<>();
        this.dataFolder = new File(plugin.getDataFolder(), "maps");
        this.mapsDataFile = new File(plugin.getDataFolder(), "maps.json");
        this.pregeneratedDataFile = new File(plugin.getDataFolder(), "pregenerated.json");
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .enableComplexMapKeySerialization()
                .registerTypeAdapter(Location.class, new JsonSerializer<Location>() {
                    @Override
                    public JsonElement serialize(Location src, Type typeOfSrc, JsonSerializationContext context) {
                        JsonObject json = new JsonObject();
                        json.addProperty("world", src.getWorld().getName());
                        json.addProperty("x", src.getX());
                        json.addProperty("y", src.getY());
                        json.addProperty("z", src.getZ());
                        json.addProperty("yaw", src.getYaw());
                        json.addProperty("pitch", src.getPitch());
                        return json;
                    }
                })
                .registerTypeAdapter(Location.class, new JsonDeserializer<Location>() {
                    @Override
                    public Location deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                        JsonObject jsonObject = json.getAsJsonObject();
                        return new Location(
                                org.bukkit.Bukkit.getWorld(jsonObject.get("world").getAsString()),
                                jsonObject.get("x").getAsDouble(),
                                jsonObject.get("y").getAsDouble(),
                                jsonObject.get("z").getAsDouble(),
                                jsonObject.get("yaw").getAsFloat(),
                                jsonObject.get("pitch").getAsFloat()
                        );
                    }
                })
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
                .registerTypeAdapter(BlockVector3.class, new JsonSerializer<BlockVector3>() {
                    @Override
                    public JsonElement serialize(BlockVector3 src, Type typeOfSrc, JsonSerializationContext context) {
                        JsonObject json = new JsonObject();
                        json.addProperty("x", src.getX());
                        json.addProperty("y", src.getY());
                        json.addProperty("z", src.getZ());
                        return json;
                    }
                })
                .registerTypeAdapter(BlockVector3.class, new JsonDeserializer<BlockVector3>() {
                    @Override
                    public BlockVector3 deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                        JsonObject jsonObject = json.getAsJsonObject();
                        return BlockVector3.at(
                                jsonObject.get("x").getAsInt(),
                                jsonObject.get("y").getAsInt(),
                                jsonObject.get("z").getAsInt()
                        );
                    }
                })
                .create();

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public void createMap(Player player, String name, Material icon) {
        if (maps.size() >= 25) {
            player.sendMessage(ChatColor.RED + "Maximum number of maps (25) reached!");
            return;
        }
        try {
            Region selection = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player)).getSelection(BukkitAdapter.adapt(player.getWorld()));

            if (selection == null) {
                player.sendMessage(ChatColor.RED + "You must make a WorldEdit selection first!");
                return;
            }

            BlockArrayClipboard clipboard = new BlockArrayClipboard(selection);
            EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(player.getWorld()));
            ForwardExtentCopy copy = new ForwardExtentCopy(editSession, selection, clipboard, selection.getMinimumPoint());
            Operations.complete(copy);
            editSession.close();

            File schematicFile = new File(dataFolder, name + ".schem");
            try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(schematicFile))) {
                writer.write(clipboard);
            }

            BlockVector3 min = selection.getMinimumPoint();
            BlockVector3 max = selection.getMaximumPoint();
            int centerX = (min.getX() + max.getX()) / 2;

            int spawn1Z = min.getZ() + 2;
            int spawn2Z = max.getZ() - 2;

            if (spawn1Z < min.getZ() + 1) spawn1Z = min.getZ() + 1;
            if (spawn1Z > max.getZ() - 1) spawn1Z = max.getZ() - 1;
            if (spawn2Z > max.getZ() - 1) spawn2Z = max.getZ() - 1;
            if (spawn2Z < min.getZ() + 1) spawn2Z = min.getZ() + 1;

            int spawn1Y = min.getY() + 1;
            int spawn2Y = min.getY() + 1;

            World world = player.getWorld();
            for (int y = max.getY(); y >= min.getY(); y--) {
                if (world.getBlockAt(centerX, y, spawn1Z).getType().isSolid()) {
                    spawn1Y = y + 1;
                    break;
                }
            }
            for (int y = max.getY(); y >= min.getY(); y--) {
                if (world.getBlockAt(centerX, y, spawn2Z).getType().isSolid()) {
                    spawn2Y = y + 1;
                    break;
                }
            }

            DuelMap map = new DuelMap(name, icon, schematicFile.getName(), min, max, null, null);
            maps.put(name.toLowerCase(), map);
            saveMaps();

            player.sendMessage(ChatColor.GREEN + "Map '" + name + "' created successfully!");
            player.sendMessage(ChatColor.YELLOW + "Note: You must set spawn 1 and spawn 2 using /dueladmin setspawn1/2 before this map can be used.");
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Failed to create map: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public PregeneratedArena getAvailableArena(DuelMap map) {
        List<PregeneratedArena> arenas = pregeneratedArenas.get(map.name().toLowerCase());
        if (arenas == null || arenas.isEmpty()) {
            return pregenerateArenaSync(map);
        }

        synchronized (arenas) {
            for (PregeneratedArena arena : arenas) {
                if (!arena.isInUse()) {
                    arena.setInUse(true);
                    replenishArenasAsync(map);
                    return arena;
                }
            }
        }

        return pregenerateArenaSync(map);
    }

    private PregeneratedArena pregenerateArenaSync(DuelMap map) {
        String arenaId = UUID.randomUUID().toString();
        Location[] spawnPoints = pasteSchematic(map, arenaId);
        if (spawnPoints == null) return null;
        PregeneratedArena arena = new PregeneratedArena(arenaId, spawnPoints);
        arena.setInUse(true);
        return arena;
    }

    private void replenishArenasAsync(DuelMap map) {
        new BukkitRunnable() {
            @Override
            public void run() {
                replenishArenas(map);
            }
        }.runTaskAsynchronously(plugin);
    }

    public void replenishArenas(DuelMap map) {
        if (!map.isSpawnsSet()) return;
        
        List<PregeneratedArena> arenas = pregeneratedArenas.computeIfAbsent(map.name().toLowerCase(), k -> Collections.synchronizedList(new ArrayList<>()));
        
        long availableCount;
        synchronized (arenas) {
            availableCount = arenas.stream().filter(a -> !a.isInUse()).count();
        }

        while (availableCount < 3) {
            String arenaId = UUID.randomUUID().toString();
            Location[] spawnPoints = pasteSchematic(map, arenaId);
            if (spawnPoints != null) {
                PregeneratedArena arena = new PregeneratedArena(arenaId, spawnPoints);
                arenas.add(arena);
                savePregeneratedArenas();

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        keepLoaded(spawnPoints[0]);
                        keepLoaded(spawnPoints[1]);
                    }
                }.runTask(plugin);
                
                availableCount++;
            } else {
                break;
            }
        }
    }

    private void keepLoaded(Location loc) {
        World world = loc.getWorld();
        if (world != null) {
            world.setChunkForceLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4, true);
        }
    }

    private Location[] pasteSchematic(DuelMap map, String arenaId) {
        try {
            File schematicFile = new File(dataFolder, map.schematicFile());
            Clipboard clipboard;

            try (ClipboardReader reader = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getReader(new FileInputStream(schematicFile))) {
                clipboard = reader.read();
            }

            World duelsWorld = plugin.getWorldManager().getDuelsWorld();


            int mapHash = Math.abs(map.name().toLowerCase().hashCode() % 50);
            int arenaHash = Math.abs(arenaId.hashCode() % 100);
            int offsetX = mapHash * 500;
            int offsetZ = arenaHash * 500;
            
            BlockVector3 pasteLocation = BlockVector3.at(offsetX, 100, offsetZ);

            EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(duelsWorld));
            ForwardExtentCopy copy = new ForwardExtentCopy(clipboard, clipboard.getRegion(), editSession, pasteLocation);
            Operations.complete(copy);
            editSession.close();

            BlockVector3 spawn1Offset = map.spawn1().subtract(map.min());
            BlockVector3 spawn2Offset = map.spawn2().subtract(map.min());

            BlockVector3 actualSpawn1 = pasteLocation.add(spawn1Offset);
            BlockVector3 actualSpawn2 = pasteLocation.add(spawn2Offset);

            Location loc1 = new Location(duelsWorld, actualSpawn1.getX() + 0.5, actualSpawn1.getY(), actualSpawn1.getZ() + 0.5);
            Location loc2 = new Location(duelsWorld, actualSpawn2.getX() + 0.5, actualSpawn2.getY(), actualSpawn2.getZ() + 0.5);

            return new Location[]{findSafeSpawn(loc1), findSafeSpawn(loc2)};
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Location findSafeSpawn(Location loc) {
        Location safe = loc.clone();
        while (safe.getY() < 254 && (safe.getBlock().getType().isSolid() || safe.clone().add(0, 1, 0).getBlock().getType().isSolid())) {
            safe.add(0, 0.5, 0);
        }
        return safe;
    }

    public void pregenerateAll() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (DuelMap map : maps.values()) {
                    replenishArenas(map);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void deleteArena(DuelMap map, String arenaId) {
        try {
            World duelsWorld = plugin.getWorldManager().getDuelsWorld();

            int mapHash = Math.abs(map.name().toLowerCase().hashCode() % 50);
            int arenaHash = Math.abs(arenaId.hashCode() % 100);
            int offsetX = mapHash * 500;
            int offsetZ = arenaHash * 500;
            
            BlockVector3 pasteLocation = BlockVector3.at(offsetX, 100, offsetZ);

            BlockVector3 size = map.max().subtract(map.min());
            BlockVector3 deleteMin = pasteLocation;
            BlockVector3 deleteMax = pasteLocation.add(size);

            EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(duelsWorld));
            CuboidRegion region = new CuboidRegion(BukkitAdapter.adapt(duelsWorld), deleteMin, deleteMax);
            editSession.setBlocks((Region) region, BukkitAdapter.adapt(Material.AIR.createBlockData()));
            editSession.close();

            new BukkitRunnable() {
                @Override
                public void run() {
                    Location loc1 = new Location(duelsWorld, deleteMin.getX(), deleteMin.getY(), deleteMin.getZ());
                    Location loc2 = new Location(duelsWorld, deleteMax.getX(), deleteMax.getY(), deleteMax.getZ());
                    duelsWorld.setChunkForceLoaded(loc1.getBlockX() >> 4, loc1.getBlockZ() >> 4, false);
                    duelsWorld.setChunkForceLoaded(loc2.getBlockX() >> 4, loc2.getBlockZ() >> 4, false);
                }
            }.runTask(plugin);

            List<PregeneratedArena> arenas = pregeneratedArenas.get(map.name().toLowerCase());
            if (arenas != null) {
                arenas.removeIf(a -> a.getArenaId().equals(arenaId));
                savePregeneratedArenas();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadPregeneratedArenas() {
        if (!pregeneratedDataFile.exists()) {
            return;
        }

        try (Reader reader = new FileReader(pregeneratedDataFile)) {
            Type type = new TypeToken<Map<String, List<PregeneratedArena>>>(){}.getType();
            Map<String, List<PregeneratedArena>> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                pregeneratedArenas.putAll(loaded);
                for (List<PregeneratedArena> arenas : pregeneratedArenas.values()) {
                    for (PregeneratedArena arena : arenas) {
                        arena.setInUse(false);
                        keepLoaded(arena.getSpawnPoints()[0]);
                        keepLoaded(arena.getSpawnPoints()[1]);
                    }
                }
            }
            plugin.getLogger().info("Loaded pregenerated arenas");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load pregenerated arenas: " + e.getMessage());
        }
    }

    public void savePregeneratedArenas() {
        try (Writer writer = new FileWriter(pregeneratedDataFile)) {
            gson.toJson(pregeneratedArenas, writer);
            writer.flush();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save pregenerated arenas: " + e.getMessage());
        }
    }

    public Map<String, Integer> getPregeneratedArenasCount() {
        Map<String, Integer> counts = new HashMap<>();
        pregeneratedArenas.forEach((mapName, arenas) -> {
            counts.put(mapName, arenas.size());
        });
        return counts;
    }

    public DuelMap getMap(String name) {
        return maps.get(name.toLowerCase());
    }

    public Collection<DuelMap> getAllMaps() {
        return maps.values();
    }

    public void loadMaps() {
        if (!mapsDataFile.exists()) {
            return;
        }

        try (Reader reader = new FileReader(mapsDataFile)) {
            Map<String, DuelMap> loadedMaps = gson.fromJson(reader, new TypeToken<Map<String, DuelMap>>(){}.getType());
            if (loadedMaps != null) {
                maps.putAll(loadedMaps);
            }
            plugin.getLogger().info("Loaded " + maps.size() + " maps");
        } catch (JsonSyntaxException e) {
            plugin.getLogger().severe("Failed to load maps: JSON file is corrupted. Deleting and starting fresh.");
            plugin.getLogger().severe("Error: " + e.getMessage());
            mapsDataFile.delete();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load maps: " + e.getMessage());
        }
    }

    public void saveMaps() {
        try {
            if (!mapsDataFile.getParentFile().exists()) {
                mapsDataFile.getParentFile().mkdirs();
            }

            try (Writer writer = new FileWriter(mapsDataFile)) {
                gson.toJson(maps, writer);
                writer.flush();
                plugin.getLogger().info("Saved " + maps.size() + " maps");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save maps: " + e.getMessage());
        }
    }

    public void deleteMap(String name) {
        DuelMap map = maps.remove(name.toLowerCase());
        if (map != null) {
            List<PregeneratedArena> arenas = pregeneratedArenas.remove(name.toLowerCase());
            if (arenas != null) {
                for (PregeneratedArena arena : arenas) {
                    deleteArena(map, arena.getArenaId());
                }
            }
            savePregeneratedArenas();

            File schematicFile = new File(dataFolder, map.schematicFile());
            if (schematicFile.exists()) {
                schematicFile.delete();
            }
            saveMaps();
        }
    }

    public void setSpawn1(String mapName, BlockVector3 spawn1) {
        DuelMap oldMap = maps.get(mapName.toLowerCase());
        if (oldMap == null) return;

        DuelMap newMap = new DuelMap(oldMap.name(), oldMap.icon(), oldMap.schematicFile(),
                oldMap.min(), oldMap.max(), spawn1, oldMap.spawn2());
        maps.put(mapName.toLowerCase(), newMap);
        saveMaps();
        regenerateArenas(newMap);
    }

    public void setSpawn2(String mapName, BlockVector3 spawn2) {
        DuelMap oldMap = maps.get(mapName.toLowerCase());
        if (oldMap == null) return;

        DuelMap newMap = new DuelMap(oldMap.name(), oldMap.icon(), oldMap.schematicFile(),
                oldMap.min(), oldMap.max(), oldMap.spawn1(), spawn2);
        maps.put(mapName.toLowerCase(), newMap);
        saveMaps();
        regenerateArenas(newMap);
    }

    private void regenerateArenas(DuelMap map) {
        List<PregeneratedArena> arenas = pregeneratedArenas.remove(map.name().toLowerCase());
        if (arenas != null) {
            for (PregeneratedArena arena : arenas) {
                deleteArena(map, arena.getArenaId());
            }
        }
        savePregeneratedArenas();
        replenishArenasAsync(map);
    }
}