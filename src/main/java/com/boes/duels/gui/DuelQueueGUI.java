package com.boes.duels.gui;

import com.boes.duels.Duels;
import com.boes.duels.data.DuelQueue;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class DuelQueueGUI {
    private final Duels plugin;
    private final Player player;
    private final Inventory inventory;

    public DuelQueueGUI(Duels plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(null, 27, ChatColor.GOLD + "Duel Queues");

        setupInventory();
    }

    public void setupInventory() {
        inventory.clear();
        int slot = 0;
        for (DuelQueue queue : plugin.getQueueManager().getAllQueues()) {
            if (slot >= 27) break;

            ItemStack item = queue.displayItem().clone();
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + queue.name());

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Kit: " + ChatColor.YELLOW + queue.kitName());
            lore.add(ChatColor.GRAY + "Map: " + ChatColor.YELLOW + queue.mapName());
            lore.add("");
            lore.add(ChatColor.GREEN + "Click to join this queue!");
            meta.setLore(lore);

            item.setItemMeta(meta);
            inventory.setItem(slot++, item);
        }
    }

    public void open() {
        player.openInventory(inventory);
    }

    public Inventory getInventory() {
        return inventory;
    }
}
