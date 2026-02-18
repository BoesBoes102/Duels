package com.boes.duels.data;

import org.bukkit.Location;

public class PregeneratedArena {
    private final String arenaId;
    private final Location[] spawnPoints;
    private boolean inUse;

    public PregeneratedArena(String arenaId, Location[] spawnPoints) {
        this.arenaId = arenaId;
        this.spawnPoints = spawnPoints;
        this.inUse = false;
    }

    public String getArenaId() {
        return arenaId;
    }

    public Location[] getSpawnPoints() {
        return spawnPoints;
    }

    public boolean isInUse() {
        return !inUse;
    }

    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }
}
