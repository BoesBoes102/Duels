package com.boes.duels.managers;

import com.boes.duels.Duels;
import org.bukkit.*;
import org.bukkit.generator.ChunkGenerator;
import org.jspecify.annotations.NonNull;

import java.util.Random;

public class WorldManager {
    private final Duels plugin;
    private World duelsWorld;

    public WorldManager(Duels plugin) {
        this.plugin = plugin;
    }

    public void createDuelsWorld() {
        WorldCreator creator = new WorldCreator("Duels");
        creator.environment(World.Environment.NORMAL);
        creator.generator(new VoidGenerator());
        creator.generateStructures(false);
        duelsWorld = creator.createWorld();

        if (duelsWorld != null) {
            duelsWorld.setDifficulty(Difficulty.NORMAL);
            duelsWorld.setSpawnFlags(false, false);
            duelsWorld.setPVP(true);
            duelsWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            duelsWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            duelsWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            duelsWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            duelsWorld.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
            duelsWorld.setTime(6000);

            plugin.getLogger().info("Duels world created/loaded successfully!");
        }
    }

    public World getDuelsWorld() {
        return duelsWorld;
    }

    public static class VoidGenerator extends ChunkGenerator {
        @Override
        public @NonNull ChunkData generateChunkData(@NonNull World world, @NonNull Random random, int x, int z, @NonNull BiomeGrid biome) {
            return createChunkData(world);
        }
    }
}