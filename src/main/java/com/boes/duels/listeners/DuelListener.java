package com.boes.duels.listeners;

import com.boes.duels.Duels;
import com.boes.duels.data.ActiveDuel;
import com.boes.duels.data.Kit;
import com.boes.duels.data.DuelMap;
import com.boes.duels.gui.KitSelectionGUI;
import com.boes.duels.gui.MapSelectionGUI;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DuelListener implements Listener {
    private final Duels plugin;
    private final Map<UUID, MapSelectionGUI> wagerInputWaiting = new HashMap<>();
    private final Map<UUID, KitSelectionGUI> kitSelectionGUIMap = new HashMap<>();
    private final Map<UUID, MapSelectionGUI> mapSelectionGUIMap = new HashMap<>();

    public DuelListener(Duels plugin) {
        this.plugin = plugin;
    }

    public void registerKitSelectionGUI(Player player, KitSelectionGUI gui) {
        kitSelectionGUIMap.put(player.getUniqueId(), gui);
    }

    public void registerMapSelectionGUI(Player player, MapSelectionGUI gui) {
        mapSelectionGUIMap.put(player.getUniqueId(), gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory == null || event.getCurrentItem() == null) return;

        String title = event.getView().getTitle();

        if (title.startsWith(ChatColor.AQUA + "Kit Preview:")) {
            event.setCancelled(true);
            return;
        }

        if (title.equals(ChatColor.GOLD + "Select a Kit")) {
            event.setCancelled(true);

            if (event.getCurrentItem().getItemMeta() == null) return;

            String kitName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
            Kit kit = plugin.getKitManager().getKit(kitName);

            if (kit == null) return;

            KitSelectionGUI gui = kitSelectionGUIMap.get(player.getUniqueId());
            if (gui == null) {
                player.closeInventory();
                return;
            }

            Player target = gui.getTarget();
            player.closeInventory();
            MapSelectionGUI mapGui = new MapSelectionGUI(plugin, player, target, kit);
            registerMapSelectionGUI(player, mapGui);
            mapGui.open();

        } else if (title.equals(ChatColor.GOLD + "Select a Map")) {
            event.setCancelled(true);

            if (event.getCurrentItem().getItemMeta() == null) return;

            String itemName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

            if (itemName.startsWith("Set Wager")) {
                MapSelectionGUI mapGui = mapSelectionGUIMap.get(player.getUniqueId());
                if (mapGui == null) {
                    player.closeInventory();
                    return;
                }

                if (mapGui.getTarget() == null) {
                    player.sendMessage(ChatColor.RED + "You cannot wager in a practice duel!");
                    return;
                }

                player.closeInventory();
                wagerInputWaiting.put(player.getUniqueId(), mapGui);
                player.sendMessage(ChatColor.YELLOW + "Type the wager amount in chat (or type 'cancel' to go back):");
                return;
            }

            String mapName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
            DuelMap map = plugin.getMapManager().getMap(mapName);

            if (map == null) return;

            if (map.isSpawnsSet()) {
                player.sendMessage(ChatColor.RED + "This map is not fully setup yet! (Spawns not set)");
                return;
            }

            MapSelectionGUI mapGui = mapSelectionGUIMap.get(player.getUniqueId());
            if (mapGui == null) {
                player.closeInventory();
                return;
            }

            Player target = mapGui.getTarget();
            Kit kit = mapGui.getKit();

            if (kit == null) {
                player.closeInventory();
                return;
            }

            player.closeInventory();
            mapSelectionGUIMap.remove(player.getUniqueId());
            
            if (target == null) {
                plugin.getDuelManager().startPracticeDuel(player, kit, map);
            } else {
                plugin.getDuelManager().sendDuelRequest(player, target, kit, map, mapGui.getWager());
            }
        } else if (title.equals(ChatColor.GOLD + "Duel Queues")) {
            event.setCancelled(true);

            if (event.getCurrentItem().getItemMeta() == null) return;

            String queueName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
            player.closeInventory();
            plugin.getQueueManager().joinQueue(player, queueName);
        }
    }

    @EventHandler
    public void onEntityResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Zombie)) return;
        Zombie zombie = (Zombie) event.getEntity();

        for (ActiveDuel duel : plugin.getDuelManager().getActiveDuels().values()) {
            if (zombie.equals(duel.getZombie())) {
                zombie.getEquipment().setItemInOffHand(new ItemStack(Material.TOTEM_OF_UNDYING, 2));
                return;
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie)) return;
        Zombie zombie = (Zombie) event.getEntity();

        for (ActiveDuel duel : plugin.getDuelManager().getActiveDuels().values()) {
            if (zombie.equals(duel.getZombie())) {
                event.getDrops().clear();
                event.setDroppedExp(0);
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        ActiveDuel duel = plugin.getDuelManager().getDuel(player);

        if (duel == null) return;

        event.setDeathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);

        if (duel.isPractice()) {
            plugin.getDuelManager().endPracticeDuel(duel);
        } else {
            Player opponent = duel.getOpponent(player);
            plugin.getDuelManager().endDuel(duel, opponent, player);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getKitEditorManager().isEditing(player)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                event.setTo(from);
            }
            return;
        }

        ActiveDuel duel = plugin.getDuelManager().getDuel(player);

        if (duel == null) return;

        if (duel.isCountdownInProgress()) {
            event.setCancelled(true);
            return;
        }

        if (!duel.isInArena(event.getTo())) {
            if (player.equals(duel.getPlayer1())) {
                player.teleport(duel.getSpawnPoints()[0]);
            } else if (player.equals(duel.getPlayer2())) {
                player.teleport(duel.getSpawnPoints()[1]);
            }
            player.sendMessage(ChatColor.RED + "You cannot leave the arena during a duel!");
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();

        if (plugin.getDuelManager().isInDuel(player)) {
            if (command.startsWith("/duel practiceleave")) {
                return;
            }
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot use commands during a duel!");
            return;
        }

        if (plugin.getKitEditorManager().isEditing(player)) {
            if (command.startsWith("/duel kiteditor save") || command.startsWith("/duel kiteditor cancel")) {
                return;
            }
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot use commands while editing a kit!");
        }
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (plugin.getDuelManager().isInDuel(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot change your gamemode during a duel!");
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (plugin.getKitEditorManager().isEditing(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean restored = plugin.getPlayerDataManager().restorePlayerData(player);
        if (!restored && player.getWorld().getName().equals("Duels")) {
            player.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
            player.sendMessage(ChatColor.RED + "You were in a duel that ended. You have been returned to spawn.");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ActiveDuel duel = plugin.getDuelManager().getDuel(player);

        if (duel != null) {
            plugin.getPlayerDataManager().restorePlayerData(player, false);
            if (duel.isPractice()) {
                plugin.getDuelManager().endPracticeDuel(duel);
            } else {
                Player opponent = duel.getOpponent(player);
                plugin.getDuelManager().endDuel(duel, opponent, player);
                opponent.sendMessage(ChatColor.GREEN + "Your opponent disconnected. You win!");
            }
            return;
        }

        if (plugin.getKitEditorManager().isEditing(player)) {
            plugin.getKitEditorManager().saveEditing(player);
        }

        wagerInputWaiting.remove(player.getUniqueId());
        kitSelectionGUIMap.remove(player.getUniqueId());
        mapSelectionGUIMap.remove(player.getUniqueId());
        plugin.getQueueManager().leaveQueue(player);
        plugin.getDuelManager().removePendingRequest(player);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        if (!wagerInputWaiting.containsKey(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        
        MapSelectionGUI mapGui = wagerInputWaiting.remove(player.getUniqueId());
        String message = event.getMessage();

        if (message.equalsIgnoreCase("cancel")) {
            new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    mapGui.open();
                }
            }.runTask(plugin);
            return;
        }
        
        try {
            double wager = Double.parseDouble(message);
            if (wager < 0) {
                player.sendMessage(ChatColor.RED + "Wager amount cannot be negative!");
                wagerInputWaiting.put(player.getUniqueId(), mapGui);
                return;
            }

            if (wager > 0 && !plugin.getVaultManager().hasBalance(player, wager)) {
                player.sendMessage(ChatColor.RED + "You don't have enough money for this wager!");
                wagerInputWaiting.put(player.getUniqueId(), mapGui);
                return;
            }

            if (wager > 0 && !plugin.getVaultManager().hasBalance(mapGui.getTarget(), wager)) {
                player.sendMessage(ChatColor.RED + mapGui.getTarget().getName() + " doesn't have enough money for this wager!");
                wagerInputWaiting.put(player.getUniqueId(), mapGui);
                return;
            }

            mapGui.setWager(wager);
            
            new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    mapGui.open();
                }
            }.runTask(plugin);
            
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid wager amount! Please enter a valid number or type 'cancel'.");
            wagerInputWaiting.put(player.getUniqueId(), mapGui);
        }
    }
}