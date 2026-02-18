package com.boes.duels.data;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;

public class ActiveDuel {
    private final Player player1;
    private final Player player2;
    private final Kit kit;
    private final DuelMap map;
    private final String arenaId;
    private final Location[] spawnPoints;
    private final double wager;
    private boolean countdownInProgress;
    private final boolean isPractice;
    private Zombie zombie;

    public ActiveDuel(Player player1, Player player2, Kit kit, DuelMap map, String arenaId, Location[] spawnPoints, double wager) {
        this.player1 = player1;
        this.player2 = player2;
        this.kit = kit;
        this.map = map;
        this.arenaId = arenaId;
        this.spawnPoints = spawnPoints;
        this.wager = wager;
        this.countdownInProgress = true;
        this.isPractice = false;
    }

    public ActiveDuel(Player player1, Kit kit, DuelMap map, String arenaId, Location[] spawnPoints) {
        this.player1 = player1;
        this.player2 = null;
        this.kit = kit;
        this.map = map;
        this.arenaId = arenaId;
        this.spawnPoints = spawnPoints;
        this.wager = 0;
        this.countdownInProgress = true;
        this.isPractice = true;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public Player getOpponent(Player player) {
        if (isPractice) return null;
        return player.equals(player1) ? player2 : player1;
    }

    public Kit getKit() {
        return kit;
    }

    public DuelMap getMap() {
        return map;
    }

    public String getArenaId() {
        return arenaId;
    }

    public Location[] getSpawnPoints() {
        return spawnPoints;
    }

    public boolean isInArena(Location location) {
        if (!location.getWorld().equals(spawnPoints[0].getWorld())) {
            return false;
        }

        double minX = Math.min(spawnPoints[0].getX(), spawnPoints[1].getX()) - 50;
        double maxX = Math.max(spawnPoints[0].getX(), spawnPoints[1].getX()) + 50;
        double minZ = Math.min(spawnPoints[0].getZ(), spawnPoints[1].getZ()) - 50;
        double maxZ = Math.max(spawnPoints[0].getZ(), spawnPoints[1].getZ()) + 50;
        double minY = Math.min(spawnPoints[0].getY(), spawnPoints[1].getY()) - 10;
        double maxY = Math.max(spawnPoints[0].getY(), spawnPoints[1].getY()) + 50;

        return location.getX() >= minX && location.getX() <= maxX &&
                location.getY() >= minY && location.getY() <= maxY &&
                location.getZ() >= minZ && location.getZ() <= maxZ;
    }

    public boolean isCountdownInProgress() {
        return countdownInProgress;
    }

    public void setCountdownInProgress(boolean value) {
        this.countdownInProgress = value;
    }

    public double getWager() {
        return wager;
    }

    public boolean isPractice() {
        return isPractice;
    }

    public Zombie getZombie() {
        return zombie;
    }

    public void setZombie(Zombie zombie) {
        this.zombie = zombie;
    }
}