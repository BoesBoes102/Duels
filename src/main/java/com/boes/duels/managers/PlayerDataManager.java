package com.boes.duels.managers;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {
    private final Map<UUID, PlayerData> savedData;

    public PlayerDataManager() {
        this.savedData = new HashMap<>();
    }

    public void savePlayerData(Player player) {
        PlayerData data = new PlayerData();
        data.inventory = deepClone(player.getInventory().getContents());
        data.armor = deepClone(player.getInventory().getArmorContents());
        data.offhand = player.getInventory().getItemInOffHand() != null ? player.getInventory().getItemInOffHand().clone() : null;
        data.location = player.getLocation().clone();
        data.health = player.getHealth();
        data.foodLevel = player.getFoodLevel();
        data.saturation = player.getSaturation();
        data.exp = player.getExp();
        data.level = player.getLevel();
        data.gameMode = player.getGameMode();
        data.allowFlight = player.getAllowFlight();
        data.flying = player.isFlying();
        data.potionEffects = player.getActivePotionEffects();

        savedData.put(player.getUniqueId(), data);

        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(10.0f);
        player.setExp(0);
        player.setLevel(0);
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    private ItemStack[] deepClone(ItemStack[] items) {
        if (items == null) return new ItemStack[0];
        ItemStack[] cloned = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                cloned[i] = items[i].clone();
            }
        }
        return cloned;
    }

    public boolean restorePlayerData(Player player) {
        return restorePlayerData(player, true);
    }

    public boolean restorePlayerData(Player player, boolean remove) {
        PlayerData data = remove ? savedData.remove(player.getUniqueId()) : savedData.get(player.getUniqueId());
        if (data == null) return false;

        player.getInventory().setContents(data.inventory);
        player.getInventory().setArmorContents(data.armor);
        player.getInventory().setItemInOffHand(data.offhand);
        player.teleport(data.location);
        player.setHealth(data.health);
        player.setFoodLevel(data.foodLevel);
        player.setSaturation(data.saturation);
        player.setExp(data.exp);
        player.setLevel(data.level);
        player.setGameMode(data.gameMode);
        player.setAllowFlight(data.allowFlight);
        player.setFlying(data.flying);

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        for (PotionEffect effect : data.potionEffects) {
            player.addPotionEffect(effect);
        }
        return true;
    }

    private static class PlayerData {
        ItemStack[] inventory;
        ItemStack[] armor;
        ItemStack offhand;
        Location location;
        double health;
        int foodLevel;
        float saturation;
        float exp;
        int level;
        GameMode gameMode;
        boolean allowFlight;
        boolean flying;
        Collection<PotionEffect> potionEffects;
    }
}