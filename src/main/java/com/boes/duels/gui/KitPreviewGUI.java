package com.boes.duels.gui;

import com.boes.duels.data.Kit;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class KitPreviewGUI {
    private final Player player;
    private final Kit kit;
    private final Inventory inventory;

    public KitPreviewGUI(Player player, Kit kit) {
        this.player = player;
        this.kit = kit;
        this.inventory = Bukkit.createInventory(null, 45, ChatColor.AQUA + "Kit Preview: " + kit.name());

        setupInventory();
    }

    private void setupInventory() {
        ItemStack[] armor = kit.armor();
        ItemStack offhand = kit.offhand();
        ItemStack[] contents = kit.contents();

        inventory.setItem(0, armor[3]);
        inventory.setItem(1, armor[2]);
        inventory.setItem(2, armor[1]);
        inventory.setItem(3, armor[0]);
        inventory.setItem(4, offhand);

        for (int i = 5; i < 9; i++) {
            ItemStack blocker = new ItemStack(Material.BARRIER);
            ItemMeta meta = blocker.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "Blocked");
            blocker.setItemMeta(meta);
            inventory.setItem(i, blocker);
        }

        for (int i = 0; i < contents.length; i++) {
            inventory.setItem(i + 9, contents[i]);
        }
    }

    public void open() {
        player.openInventory(inventory);
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Kit getKit() {
        return kit;
    }
}
