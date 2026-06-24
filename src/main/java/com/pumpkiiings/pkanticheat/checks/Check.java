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

    protected void setback(Player player) {
        if (player.level().isClientSide() || !(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }
        
        com.pumpkiiings.pkanticheat.data.PlayerData data = com.pumpkiiings.pkanticheat.data.PlayerDataManager.getData(player);
        net.minecraft.world.phys.Vec3 safeLocation = data.getLastValidGroundLocation();
        
        if (safeLocation != null && safeLocation != net.minecraft.world.phys.Vec3.ZERO) {
            // Detiene el momentum completamente en servidor
            serverPlayer.setDeltaMovement(0, 0, 0);
            serverPlayer.connection.teleport(safeLocation.x, safeLocation.y, safeLocation.z, serverPlayer.getYRot(), serverPlayer.getXRot());
        }
    }
}
