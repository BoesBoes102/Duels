package com.boes.duels.data;

import org.bukkit.inventory.ItemStack;

public record KitLayout(ItemStack[] contents, ItemStack[] armor, ItemStack offhand) {
    public KitLayout(ItemStack[] contents, ItemStack[] armor, ItemStack offhand) {
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
