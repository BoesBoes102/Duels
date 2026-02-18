package com.boes.duels.gui;

import com.boes.duels.Duels;
import com.boes.duels.data.Kit;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class KitSelectionGUI {
    private final Duels plugin;
    private final Player player;
    private final Player target;
    private final Inventory inventory;

    public KitSelectionGUI(Duels plugin, Player player, Player target) {
        this.plugin = plugin;
        this.player = player;
        this.target = target;
        this.inventory = Bukkit.createInventory(null, 27, ChatColor.GOLD + "Select a Kit");

        setupInventory();
    }

    private void setupInventory() {
        int slot = 0;
        for (Kit kit : plugin.getKitManager().getAllKits()) {
            if (slot >= 27) break;

            ItemStack item = new ItemStack(kit.icon());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + kit.name());

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Click to select this kit");
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

    public Player getPlayer() {
        return player;
    }

    public Player getTarget() {
        return target;
    }
}