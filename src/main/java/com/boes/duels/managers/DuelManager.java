package com.boes.duels.managers;

import com.boes.duels.Duels;
import com.boes.duels.data.*;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.CompletableFuture;
import java.util.*;

public class DuelManager {
    private final Duels plugin;
    private final VaultManager vaultManager;
    private final Map<UUID, DuelRequest> pendingRequests;
    private final Map<UUID, ActiveDuel> activeDuels;

    public DuelManager(Duels plugin) {
        this.plugin = plugin;
        this.vaultManager = plugin.getVaultManager();
        this.pendingRequests = new HashMap<>();
        this.activeDuels = new HashMap<>();
    }

    public void sendDuelRequest(Player sender, Player target, Kit kit, DuelMap map, double wager) {
        if (wager > 0 && vaultManager.isEnabled()) {
            if (!vaultManager.hasBalance(sender, wager)) {
                sender.sendMessage(ChatColor.RED + "You don't have enough money for this wager!");
                return;
            }
            if (!vaultManager.hasBalance(target, wager)) {
                sender.sendMessage(ChatColor.RED + target.getName() + " doesn't have enough money for this wager!");
                return;
            }
        }

        DuelRequest request = new DuelRequest(sender, target, kit, map, wager);
        pendingRequests.put(target.getUniqueId(), request);

        sender.sendMessage(ChatColor.GREEN + "Duel request sent to " + target.getName() + "!");

        TextComponent message = new TextComponent(ChatColor.GOLD + sender.getName() + " has challenged you to a duel!");
        if (wager > 0) {
            TextComponent wagerComponent = new TextComponent(ChatColor.YELLOW + " (Wager: " + wager + ")");
            message.addExtra(wagerComponent);
        }

        TextComponent acceptButton = new TextComponent(ChatColor.GREEN + " [ACCEPT]");
        acceptButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel accept " + sender.getName()));
        acceptButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to accept").create()));

        TextComponent denyButton = new TextComponent(ChatColor.RED + " [DENY]");
        denyButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel deny " + sender.getName()));
        denyButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to deny").create()));

        message.addExtra(acceptButton);
        message.addExtra(denyButton);
        target.spigot().sendMessage(message);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingRequests.containsKey(target.getUniqueId())) {
                    pendingRequests.remove(target.getUniqueId());
                    if (sender.isOnline()) {
                        sender.sendMessage(ChatColor.RED + "Duel request to " + target.getName() + " expired.");
                    }
                    if (target.isOnline()) {
                        target.sendMessage(ChatColor.RED + "Duel request from " + sender.getName() + " expired.");
                    }
                }
            }
        }.runTaskLater(plugin, 600L);
    }

    public void acceptDuel(DuelRequest request) {
        Player sender = request.sender();
        Player target = request.target();
        double wager = request.wager();

        if (wager > 0 && vaultManager.isEnabled()) {
            if (!vaultManager.hasBalance(sender, wager)) {
                sender.sendMessage(ChatColor.RED + "You no longer have enough money for this wager!");
                target.sendMessage(ChatColor.RED + sender.getName() + " no longer has enough money for this wager!");
                pendingRequests.remove(target.getUniqueId());
                return;
            }
            if (!vaultManager.hasBalance(target, wager)) {
                target.sendMessage(ChatColor.RED + "You don't have enough money for this wager!");
                sender.sendMessage(ChatColor.RED + target.getName() + " doesn't have enough money for this wager!");
                pendingRequests.remove(target.getUniqueId());
                return;
            }
        }

        pendingRequests.remove(target.getUniqueId());
        startDuel(sender, target, request.kit(), request.map(), wager);
    }

    public void denyDuel(DuelRequest request) {
        pendingRequests.remove(request.target().getUniqueId());
        request.sender().sendMessage(ChatColor.RED + request.target().getName() + " denied your duel request.");
        request.target().sendMessage(ChatColor.RED + "You denied the duel request from " + request.sender().getName() + ".");
    }

    private void startDuel(Player player1, Player player2, Kit kit, DuelMap map, double wager) {
        if (!map.isSpawnsSet()) {
            player1.sendMessage(ChatColor.RED + "This map is not fully setup yet! (Spawns not set)");
            player2.sendMessage(ChatColor.RED + "This map is not fully setup yet! (Spawns not set)");
            return;
        }

        plugin.getPlayerDataManager().savePlayerData(player1);
        plugin.getPlayerDataManager().savePlayerData(player2);

        if (wager > 0 && vaultManager.isEnabled()) {
            vaultManager.withdrawMoney(player1, wager);
            vaultManager.withdrawMoney(player2, wager);
        }

        PregeneratedArena arena = plugin.getMapManager().getAvailableArena(map);

        if (arena == null) {
            player1.sendMessage(ChatColor.RED + "Failed to create arena!");
            player2.sendMessage(ChatColor.RED + "Failed to create arena!");
            if (wager > 0 && vaultManager.isEnabled()) {
                vaultManager.depositMoney(player1, wager);
                vaultManager.depositMoney(player2, wager);
            }
            plugin.getPlayerDataManager().restorePlayerData(player1);
            plugin.getPlayerDataManager().restorePlayerData(player2);
            return;
        }

        String arenaId = arena.getArenaId();
        Location[] spawnPoints = arena.getSpawnPoints();

        ActiveDuel duel = new ActiveDuel(player1, player2, kit, map, arenaId, spawnPoints, wager);
        activeDuels.put(player1.getUniqueId(), duel);
        activeDuels.put(player2.getUniqueId(), duel);

        new BukkitRunnable() {
            @Override
            public void run() {
                Location loc1 = spawnPoints[0].clone();
                Location loc2 = spawnPoints[1].clone();
                loc1.setDirection(loc2.toVector().subtract(loc1.toVector()));
                loc2.setDirection(loc1.toVector().subtract(loc2.toVector()));

                CompletableFuture<Boolean> tp1 = player1.teleportAsync(loc1);
                CompletableFuture<Boolean> tp2 = player2.teleportAsync(loc2);

                CompletableFuture.allOf(tp1, tp2).thenCompose(v -> 
                    CompletableFuture.allOf(
                        player1.loadChunksAroundAsync(),
                        player2.loadChunksAroundAsync()
                    )
                ).thenRun(() -> {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            applyKit(player1, kit);
                            applyKit(player2, kit);

                            if (wager > 0) {
                                player1.sendMessage(ChatColor.GREEN + "Duel started against " + player2.getName() + ChatColor.YELLOW + " (Wager: " + wager + ")!");
                                player2.sendMessage(ChatColor.GREEN + "Duel started against " + player1.getName() + ChatColor.YELLOW + " (Wager: " + wager + ")!");
                            } else {
                                player1.sendMessage(ChatColor.GREEN + "Duel started against " + player2.getName() + "!");
                                player2.sendMessage(ChatColor.GREEN + "Duel started against " + player1.getName() + "!");
                            }

                            startCountdown(duel);
                        }
                    }.runTask(plugin);
                });
            }
        }.runTaskLater(plugin, 10L);
    }

    public void startPracticeDuel(Player player, Kit kit, DuelMap map) {
        if (!map.isSpawnsSet()) {
            player.sendMessage(ChatColor.RED + "This map is not fully setup yet! (Spawns not set)");
            return;
        }

        plugin.getPlayerDataManager().savePlayerData(player);

        PregeneratedArena arena = plugin.getMapManager().getAvailableArena(map);

        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Failed to create arena!");
            plugin.getPlayerDataManager().restorePlayerData(player);
            return;
        }

        String arenaId = arena.getArenaId();
        Location[] spawnPoints = arena.getSpawnPoints();

        ActiveDuel duel = new ActiveDuel(player, kit, map, arenaId, spawnPoints);
        activeDuels.put(player.getUniqueId(), duel);

        new BukkitRunnable() {
            @Override
            public void run() {
                Location loc1 = spawnPoints[0].clone();
                Location loc2 = spawnPoints[1].clone();
                loc1.setDirection(loc2.toVector().subtract(loc1.toVector()));
                loc2.setDirection(loc1.toVector().subtract(loc2.toVector()));

                player.teleportAsync(loc1).thenCompose(v -> player.loadChunksAroundAsync()).thenRun(() -> {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            applyKit(player, kit);

                            Zombie zombie = (Zombie) loc2.getWorld().spawnEntity(loc2, EntityType.HUSK);
                            zombie.setBaby(false);
                            zombie.getEquipment().setItemInOffHand(new ItemStack(Material.TOTEM_OF_UNDYING));
                            zombie.setCustomName(ChatColor.RED + "Practice Bot");
                            zombie.setCustomNameVisible(true);
                            zombie.setRemoveWhenFarAway(false);
                            duel.setZombie(zombie);

                            player.sendMessage(ChatColor.GREEN + "Practice duel started!");
                            player.sendMessage(ChatColor.GREEN + "You can exit practice at any time using /duel practiceleave");
                            duel.setCountdownInProgress(false);
                        }
                    }.runTask(plugin);
                });
            }
        }.runTaskLater(plugin, 10L);
    }

    private void startCountdown(ActiveDuel duel) {
        final int[] countdown = {10};

        new BukkitRunnable() {
            @Override
            public void run() {
                if (countdown[0] > 0) {
                    String title = ChatColor.GOLD + "" + countdown[0];
                    String subtitle = ChatColor.GRAY + "Get ready!";

                    duel.getPlayer1().sendTitle(title, subtitle, 0, 20, 5);
                    if (!duel.isPractice()) {
                        duel.getPlayer2().sendTitle(title, subtitle, 0, 20, 5);
                    }

                    duel.getPlayer1().playSound(duel.getPlayer1().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    if (!duel.isPractice()) {
                        duel.getPlayer2().playSound(duel.getPlayer2().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    }

                    countdown[0]--;
                } else {
                    duel.getPlayer1().sendTitle(ChatColor.GREEN + "FIGHT!", ChatColor.GRAY + "Duel started!", 0, 20, 5);
                    if (!duel.isPractice()) {
                        duel.getPlayer2().sendTitle(ChatColor.GREEN + "FIGHT!", ChatColor.GRAY + "Duel started!", 0, 20, 5);
                    }

                    duel.getPlayer1().playSound(duel.getPlayer1().getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                    if (!duel.isPractice()) {
                        duel.getPlayer2().playSound(duel.getPlayer2().getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                    }

                    duel.setCountdownInProgress(false);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void endPracticeDuel(ActiveDuel duel) {
        activeDuels.remove(duel.getPlayer1().getUniqueId());

        if (duel.getZombie() != null) {
            duel.getZombie().remove();
        }

        if (duel.getPlayer1().isOnline()) {
            Player player = duel.getPlayer1();
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);
            player.getInventory().setItemInOffHand(null);
            player.sendTitle(ChatColor.GREEN + "PRACTICE ENDED", "", 10, 40, 10);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (duel.getPlayer1().isOnline()) {
                    plugin.getPlayerDataManager().restorePlayerData(duel.getPlayer1());
                }
                plugin.getMapManager().deleteArena(duel.getMap(), duel.getArenaId());
            }
        }.runTaskLater(plugin, 40L);
    }

    public void endDuel(ActiveDuel duel, Player winner, Player loser) {
        activeDuels.remove(duel.getPlayer1().getUniqueId());
        activeDuels.remove(duel.getPlayer2().getUniqueId());

        String winnerName = winner.getName();
        String loserName = loser.getName();

        if (winner.isOnline()) {
            winner.getInventory().clear();
            winner.getInventory().setArmorContents(new ItemStack[4]);
            winner.getInventory().setItemInOffHand(null);
            spawnWinFireworks(winner);
        }

        if (loser.isOnline()) {
            loser.getInventory().clear();
            loser.getInventory().setArmorContents(new ItemStack[4]);
            loser.getInventory().setItemInOffHand(null);
        }

        String winTitle = ChatColor.GREEN + "YOU WON!";
        String loseTitle = ChatColor.RED + "YOU LOST!";
        String winSubtitle = ChatColor.GRAY + "Against " + loserName;
        String loseSubtitle = ChatColor.GRAY + "Against " + winnerName;

        if (duel.getWager() > 0 && vaultManager.isEnabled()) {
            double totalWinnings = duel.getWager() * 2;
            vaultManager.depositMoney(winner, totalWinnings);
            winSubtitle += ChatColor.YELLOW + " (+" + totalWinnings + ")";
            loseSubtitle += ChatColor.YELLOW + " (-" + duel.getWager() + ")";
        }

        if (winner.isOnline()) {
            winner.sendTitle(winTitle, winSubtitle, 10, 70, 20);
        }
        if (loser.isOnline()) {
            loser.sendTitle(loseTitle, loseSubtitle, 10, 70, 20);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (winner.isOnline()) {
                    plugin.getPlayerDataManager().restorePlayerData(winner);
                }
                if (loser.isOnline()) {
                    plugin.getPlayerDataManager().restorePlayerData(loser);
                }

                plugin.getMapManager().deleteArena(duel.getMap(), duel.getArenaId());
            }
        }.runTaskLater(plugin, 100L);
    }

    public void endAllDuels() {
        for (ActiveDuel duel : new ArrayList<>(activeDuels.values())) {
            plugin.getPlayerDataManager().restorePlayerData(duel.getPlayer1());
            if (!duel.isPractice()) {
                plugin.getPlayerDataManager().restorePlayerData(duel.getPlayer2());
            } else if (duel.getZombie() != null) {
                duel.getZombie().remove();
            }
            plugin.getMapManager().deleteArena(duel.getMap(), duel.getArenaId());
        }
        activeDuels.clear();
    }

    public Map<UUID, ActiveDuel> getActiveDuels() {
        return activeDuels;
    }

    public boolean isInDuel(Player player) {
        return activeDuels.containsKey(player.getUniqueId());
    }

    public ActiveDuel getDuel(Player player) {
        return activeDuels.get(player.getUniqueId());
    }

    public DuelRequest getRequest(Player sender, Player target) {
        DuelRequest request = pendingRequests.get(target.getUniqueId());
        if (request != null && request.sender().equals(sender)) {
            return request;
        }
        return null;
    }

    public void removePendingRequest(Player player) {
        List<UUID> keysToRemove = new ArrayList<>();
        for (Map.Entry<UUID, DuelRequest> entry : pendingRequests.entrySet()) {
            DuelRequest request = entry.getValue();
            if (request.target().equals(player) || request.sender().equals(player)) {
                keysToRemove.add(entry.getKey());
            }
        }
        keysToRemove.forEach(pendingRequests::remove);
    }

    private void applyKit(Player player, Kit kit) {
        com.boes.duels.data.KitLayout customLayout = plugin.getKitEditorManager().getLayout(player.getUniqueId(), kit.name());
        if (customLayout != null) {
            player.getInventory().setContents(customLayout.contents());
            player.getInventory().setArmorContents(customLayout.armor());
            player.getInventory().setItemInOffHand(customLayout.offhand());
        } else {
            kit.applyToPlayer(player);
        }
    }

    private void spawnWinFireworks(Player player) {
        Location loc = player.getLocation().add(0, 3, 0);
        Firework fw = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);
        FireworkMeta fwm = fw.getFireworkMeta();

        fwm.setPower(1);
        fwm.addEffect(FireworkEffect.builder()
                .withColor(Color.GREEN)
                .withFade(Color.WHITE)
                .with(FireworkEffect.Type.BALL_LARGE)
                .trail(true)
                .flicker(true)
                .build());

        fw.setFireworkMeta(fwm);
        fw.detonate();
    }
}