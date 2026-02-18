package com.boes.duels.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.boes.duels.Duels;
import com.boes.duels.data.DuelMap;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CommandAlias("dueladmin|duelsadmin")
public class DueladminCommand extends BaseCommand {
    private final Duels plugin;

    public DueladminCommand(Duels plugin) {
        this.plugin = plugin;
    }

    @Subcommand("createkit")
    @CommandPermission("duels.admin")
    @CommandCompletion("Sword|axe @items")
    @Syntax("<name> <icon>")
    public void onCreateKit(Player player, String name, Material icon) {
        if (plugin.getKitManager().getKit(name) != null) {
            player.sendMessage(ChatColor.RED + "A kit with that name already exists!");
            return;
        }

        if (!icon.isItem()) {
            player.sendMessage(ChatColor.RED + "Invalid icon material!");
            return;
        }

        plugin.getKitManager().createKit(player, name, icon);
    }

    @Subcommand("createmap")
    @CommandPermission("duels.admin")
    @CommandCompletion("Grass|sand @items")
    @Syntax("<name> <icon>")
    public void onCreateMap(Player player, String name, Material icon) {
        if (plugin.getMapManager().getMap(name) != null) {
            player.sendMessage(ChatColor.RED + "A map with that name already exists!");
            return;
        }

        if (!icon.isItem()) {
            player.sendMessage(ChatColor.RED + "Invalid icon material!");
            return;
        }

        plugin.getMapManager().createMap(player, name, icon);
    }

    @Subcommand("deletekit")
    @CommandPermission("duels.admin")
    @CommandCompletion("@kits")
    @Syntax("<name>")
    public void onDeleteKit(Player player, String name) {
        if (plugin.getKitManager().getKit(name) == null) {
            player.sendMessage(ChatColor.RED + "A kit with that name does not exist!");
            return;
        }

        plugin.getKitManager().deleteKit(name);
        player.sendMessage(ChatColor.GREEN + "Kit '" + name + "' deleted successfully!");
    }

    @Subcommand("deletemap")
    @CommandPermission("duels.admin")
    @CommandCompletion("@maps")
    @Syntax("<name>")
    public void onDeleteMap(Player player, String name) {
        if (plugin.getMapManager().getMap(name) == null) {
            player.sendMessage(ChatColor.RED + "A map with that name does not exist!");
            return;
        }

        plugin.getMapManager().deleteMap(name);
        player.sendMessage(ChatColor.GREEN + "Map '" + name + "' deleted successfully!");
    }

    @Subcommand("listkits")
    @CommandPermission("duels.admin")
    @Syntax("")
    public void onListKits(Player player) {
        var kits = plugin.getKitManager().getAllKits();

        if (kits.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No kits available.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "=== Available Kits ===");
        for (var kit : kits) {
            player.sendMessage(ChatColor.GREEN + "- " + kit.name());
        }
    }

    @Subcommand("listmaps")
    @CommandPermission("duels.admin")
    @Syntax("")
    public void onListMaps(Player player) {
        var maps = plugin.getMapManager().getAllMaps();

        if (maps.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No maps available.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "=== Available Maps ===");
        for (var map : maps) {
            player.sendMessage(ChatColor.GREEN + "- " + map.name());
        }
    }

    @Subcommand("wager")
    @CommandPermission("duels.admin")
    @Syntax("")
    public void onWagerToggle(Player player) {
        if (!plugin.getVaultManager().isEnabled()) {
            player.sendMessage(ChatColor.RED + "Vault is not enabled on this server!");
            return;
        }

        boolean newState = !plugin.isWagerEnabled();
        plugin.setWagerEnabled(newState);

        if (newState) {
            player.sendMessage(ChatColor.GREEN + "Wager system has been enabled!");
        } else {
            player.sendMessage(ChatColor.RED + "Wager system has been disabled!");
        }
    }

    @Subcommand("createqueue")
    @CommandPermission("duels.admin")
    @CommandCompletion("@kits @maps @nothing @items")
    @Syntax("<kit> <map> <queuename> <queueitem>")
    public void onCreateQueue(Player player, String kitName, String mapName, String queueName, Material icon) {
        if (plugin.getKitManager().getKit(kitName) == null) {
            player.sendMessage(ChatColor.RED + "Kit not found!");
            return;
        }
        if (plugin.getMapManager().getMap(mapName) == null) {
            player.sendMessage(ChatColor.RED + "Map not found!");
            return;
        }
        if (plugin.getQueueManager().getQueue(queueName) != null) {
            player.sendMessage(ChatColor.RED + "A queue with that name already exists!");
            return;
        }
        if (!icon.isItem()) {
            player.sendMessage(ChatColor.RED + "Invalid icon material!");
            return;
        }

        plugin.getQueueManager().createQueue(queueName, kitName, mapName, new ItemStack(icon));
        player.sendMessage(ChatColor.GREEN + "Queue '" + queueName + "' created successfully!");
    }

    @Subcommand("deletequeue")
    @CommandPermission("duels.admin")
    @CommandCompletion("@queues")
    @Syntax("<queuename>")
    public void onDeleteQueue(Player player, String queueName) {
        if (plugin.getQueueManager().getQueue(queueName) == null) {
            player.sendMessage(ChatColor.RED + "Queue not found!");
            return;
        }

        plugin.getQueueManager().deleteQueue(queueName);
        player.sendMessage(ChatColor.GREEN + "Queue '" + queueName + "' deleted successfully!");
    }

    @Subcommand("setspawn1")
    @CommandPermission("duels.admin")
    @CommandCompletion("@maps")
    @Syntax("<map>")
    public void onSetSpawn1(Player player, String mapName) {
        DuelMap map = plugin.getMapManager().getMap(mapName);
        if (map == null) {
            player.sendMessage(ChatColor.RED + "Map not found!");
            return;
        }

        Location loc = player.getLocation();
        BlockVector3 spawn = BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        if (!isInside(spawn, map.min(), map.max())) {
            player.sendMessage(ChatColor.RED + "The spawn point must be inside the map's region!");
            return;
        }

        plugin.getMapManager().setSpawn1(mapName, spawn);
        player.sendMessage(ChatColor.GREEN + "Spawn 1 for map '" + mapName + "' set to your current location!");
    }

    @Subcommand("setspawn2")
    @CommandPermission("duels.admin")
    @CommandCompletion("@maps")
    @Syntax("<map>")
    public void onSetSpawn2(Player player, String mapName) {
        DuelMap map = plugin.getMapManager().getMap(mapName);
        if (map == null) {
            player.sendMessage(ChatColor.RED + "Map not found!");
            return;
        }

        Location loc = player.getLocation();
        BlockVector3 spawn = BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        if (!isInside(spawn, map.min(), map.max())) {
            player.sendMessage(ChatColor.RED + "The spawn point must be inside the map's region!");
            return;
        }

        plugin.getMapManager().setSpawn2(mapName, spawn);
        player.sendMessage(ChatColor.GREEN + "Spawn 2 for map '" + mapName + "' set to your current location!");
    }

    private boolean isInside(BlockVector3 pt, BlockVector3 min, BlockVector3 max) {
        return pt.getX() >= min.getX() && pt.getX() <= max.getX() &&
                pt.getY() >= min.getY() && pt.getY() <= max.getY() &&
                pt.getZ() >= min.getZ() && pt.getZ() <= max.getZ();
    }

    @Subcommand("pregencheck")
    @CommandPermission("duels.admin")
    public void onPregenCheck(Player player) {
        var counts = plugin.getMapManager().getPregeneratedArenasCount();
        if (counts.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No maps have pregenerated arenas.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "=== Pregenerated Arenas ===");
        counts.forEach((mapName, count) -> player.sendMessage(ChatColor.GREEN + "- " + mapName + ": " + count));
    }
}
