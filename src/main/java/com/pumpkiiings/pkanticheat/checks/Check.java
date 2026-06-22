package com.pumpkiiings.pkanticheat.checks;

import com.pumpkiiings.pkanticheat.AlertManager;
import net.minecraft.world.entity.player.Player;

public abstract class Check {
    private final String name;

    public Check(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    protected void fail(Player player, String details) {
        // We could log the details or send them to discord later.
        // For now, we delegate to AlertManager.
        AlertManager.addViolation(player, name);
    }
}
