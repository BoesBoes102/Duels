package com.boes.duels.data;

import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Material;

public record DuelMap(String name, Material icon, String schematicFile, BlockVector3 min, BlockVector3 max,
                      BlockVector3 spawn1, BlockVector3 spawn2) {

    public boolean isSpawnsSet() {
        return spawn1 == null || spawn2 == null;
    }
}