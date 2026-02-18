package com.boes.duels.gui;

import com.boes.duels.Duels;
import com.boes.duels.data.DuelMap;
import com.boes.duels.data.Kit;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MapSelectionGUI {
    private final Duels plugin;
    private final Player player;
    private final Player target;
    private final Kit kit;
    private final Inventory inventory;
    private double wager = 0;

    public MapSelectionGUI(Duels plugin, Player player, Player target, Kit kit) {
        this.plugin = plugin;
        this.player = player;
        this.target = target;
        this.kit = kit;
        this.inventory = Bukkit.createInventory(null, 27, ChatColor.GOLD + "Select a Map");

        setupInventory();
    }

    public void setupInventory() {
        inventory.clear();
        int slot = 0;
        for (DuelMap map : plugin.getMapManager().getAllMaps()) {
            if (slot >= 26) break;

            ItemStack item = new ItemStack(map.icon());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + map.name());

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Click to select this map");
            meta.setLore(lore);

            item.setItemMeta(meta);
            inventory.setItem(slot++, item);
        }

        if (plugin.isWagerEnabled() && target != null) {
            ItemStack wagerButton = new ItemStack(org.bukkit.Material.GOLD_NUGGET);
            ItemMeta meta = wagerButton.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "Set Wager" + (wager > 0 ? ChatColor.GREEN + " (" + wager + ")" : ""));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Current Wager: " + ChatColor.GOLD + wager);
            lore.add(ChatColor.GRAY + "Click to change wager amount");
            meta.setLore(lore);
            wagerButton.setItemMeta(meta);
            inventory.setItem(26, wagerButton);
        }
    }

    public void setWager(double wager) {
        this.wager = wager;
        setupInventory();
    }

    public double getWager() {
        return wager;
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

    public Kit getKit() {
        return kit;
    }
}
