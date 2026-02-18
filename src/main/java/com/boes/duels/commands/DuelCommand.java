package com.boes.duels.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.boes.duels.Duels;
import com.boes.duels.data.ActiveDuel;
import com.boes.duels.data.DuelRequest;
import com.boes.duels.gui.KitSelectionGUI;
import com.boes.duels.managers.DuelManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@CommandAlias("duel")
public class DuelCommand extends BaseCommand {
    private final Duels plugin;
    private final DuelManager duelManager;

    public DuelCommand(Duels plugin) {
        this.plugin = plugin;
        this.duelManager = plugin.getDuelManager();
    }

    @Default
    @CommandCompletion("@players")
    @Syntax("<player>")
    public void onDuel(Player player, String targetName) {
        Player target = plugin.getServer().getPlayer(targetName);

        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "Player not found or offline!");
            return;
        }

        if (target.equals(player)) {
            player.sendMessage(ChatColor.RED + "You cannot duel yourself!");
            return;
        }

        if (duelManager.isInDuel(player)) {
            player.sendMessage(ChatColor.RED + "You are already in a duel!");
            return;
        }

        if (duelManager.isInDuel(target)) {
            player.sendMessage(ChatColor.RED + "That player is already in a duel!");
            return;
        }

        KitSelectionGUI gui = new KitSelectionGUI(plugin, player, target);
        plugin.getDuelListener().registerKitSelectionGUI(player, gui);
        gui.open();
    }

    @Subcommand("accept")
    @CommandCompletion("@players")
    @Syntax("<player>")
    public void onAccept(Player player, String senderName) {
        Player sender = plugin.getServer().getPlayer(senderName);

        if (sender == null || !sender.isOnline()) {
            player.sendMessage(ChatColor.RED + "The player who sent the duel request is no longer online!");
            return;
        }

        DuelRequest request = duelManager.getRequest(sender, player);
        if (request == null) {
            player.sendMessage(ChatColor.RED + "You don't have a pending duel request from that player!");
            return;
        }

        duelManager.acceptDuel(request);
    }

    @Subcommand("deny")
    @CommandCompletion("@players")
    @Syntax("<player>")
    public void onDeny(Player player, String senderName) {
        Player sender = plugin.getServer().getPlayer(senderName);

        if (sender == null) {
            player.sendMessage(ChatColor.RED + "Invalid player!");
            return;
        }

        DuelRequest request = duelManager.getRequest(sender, player);
        if (request == null) {
            player.sendMessage(ChatColor.RED + "You don't have a pending duel request from that player!");
            return;
        }

        duelManager.denyDuel(request);
    }

    @Subcommand("practice")
    @CommandCompletion("")
    @Syntax("")
    public void onPractice(Player player) {
        if (duelManager.isInDuel(player)) {
            player.sendMessage(ChatColor.RED + "You are already in a duel!");
            return;
        }

        KitSelectionGUI gui = new KitSelectionGUI(plugin, player, null);
        plugin.getDuelListener().registerKitSelectionGUI(player, gui);
        gui.open();
    }

    @Subcommand("practiceleave")
    @CommandCompletion("")
    @Syntax("")
    public void onPracticeLeave(Player player) {
        ActiveDuel duel = duelManager.getDuel(player);
        if (duel == null || !duel.isPractice()) {
            player.sendMessage(ChatColor.RED + "You are not in a practice duel!");
            return;
        }

        duelManager.endPracticeDuel(duel);
    }

    @Subcommand("kiteditor")
    @CommandCompletion("@kits|save")
    @Syntax("<kitname>|save")
    public void onKitEditor(Player player, @Optional String kitName) {
        if (kitName == null || kitName.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Usage: /duel kiteditor <kitname> or /duel kiteditor save");
            return;
        }

        if (kitName.equalsIgnoreCase("save")) {
            plugin.getKitEditorManager().saveEditing(player);
            return;
        }

        if (duelManager.isInDuel(player)) {
            player.sendMessage(ChatColor.RED + "You cannot edit kits while in a duel!");
            return;
        }

        if (plugin.getKitEditorManager().isEditing(player)) {
            player.sendMessage(ChatColor.RED + "You are already editing a kit! Use /duel kiteditor save first.");
            return;
        }

        plugin.getKitEditorManager().startEditing(player, kitName);
    }
}