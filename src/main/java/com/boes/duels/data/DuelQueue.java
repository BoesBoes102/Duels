package com.boes.duels.data;

import org.bukkit.inventory.ItemStack;

public record DuelQueue(String name, String kitName, String mapName, ItemStack displayItem) {
}
