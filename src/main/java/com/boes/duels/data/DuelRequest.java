package com.boes.duels.data;

import org.bukkit.entity.Player;

public record DuelRequest(Player sender, Player target, Kit kit, DuelMap map, double wager) {
}