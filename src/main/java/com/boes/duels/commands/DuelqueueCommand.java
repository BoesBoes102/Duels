package com.boes.duels.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import com.boes.duels.Duels;
import com.boes.duels.gui.DuelQueueGUI;
import org.bukkit.entity.Player;

@CommandAlias("duelqueue")
public class DuelqueueCommand extends BaseCommand {
    private final Duels plugin;

    public DuelqueueCommand(Duels plugin) {
        this.plugin = plugin;
    }

    @Default
    public void onDefault(Player player) {
        if (plugin.getQueueManager().getAllQueues().isEmpty()) {
            player.sendMessage(org.bukkit.ChatColor.RED + "There are no active queues at the moment!");
            return;
        }
        new DuelQueueGUI(plugin, player).open();
    }
}
