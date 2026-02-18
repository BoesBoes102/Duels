package com.boes.duels.data;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public record Kit(String name, Material icon, ItemStack[] contents, ItemStack[] armor, ItemStack offhand) {
    public Kit(String name, Material icon, ItemStack[] contents, ItemStack[] armor, ItemStack offhand) {
        this.name = name;
        this.icon = icon;
        this.contents = deepClone(contents);
        this.armor = deepClone(armor);
        this.offhand = offhand != null ? offhand.clone() : null;
    }

    @Override
    public ItemStack[] contents() {
        return deepClone(contents);
    }

    @Override
    public ItemStack[] armor() {
        return deepClone(armor);
    }

    @Override
    public ItemStack offhand() {
        return offhand != null ? offhand.clone() : null;
    }

    public void applyToPlayer(Player player) {
        player.getInventory().setContents(deepClone(contents));
        player.getInventory().setArmorContents(deepClone(armor));
        if (offhand != null) {
            player.getInventory().setItemInOffHand(offhand.clone());
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
}