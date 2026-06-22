package com.pumpkiiings.pkanticheat.checks.packet;

import com.pumpkiiings.pkanticheat.AlertManager;
import com.pumpkiiings.pkanticheat.checks.Check;
import com.pumpkiiings.pkanticheat.data.PlayerData;
import com.pumpkiiings.pkanticheat.data.PlayerDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class BlinkCheck extends Check {

    public BlinkCheck() {
        super("Blink");
    }

    private static final BlinkCheck INSTANCE = new BlinkCheck();

    public static void handle(ServerPlayer player) {
        if (player.level().isClientSide()) return;

        PlayerData data = PlayerDataManager.getData(player);
        long now = System.currentTimeMillis();
        long lastTime = data.lastMovementPacketTime;
        
        data.lastMovementPacketTime = now;

        if (lastTime == 0) return;

        long delta = now - lastTime;

        // If they didn't send a packet for more than 2 seconds but suddenly send one,
        // it's a huge lag spike or Blink. 
        // We look for bursts: a gap > 1500ms followed by many packets (tracked by TimerCheck).
        // For strict Blink mitigation: if gap > 1500ms, rubberband them back to prevent teleportation exploits.
        if (delta > 1500) {
            INSTANCE.fail(player, "Suspicious packet delay/burst (" + delta + "ms)");
            
            // Setback logic for Blink (Teleport to original location before blink)
            Vec3 safePos = data.getLastValidGroundLocation();
            if (safePos != Vec3.ZERO) {
                player.connection.teleport(safePos.x, safePos.y, safePos.z, player.getYRot(), player.getXRot());
            }
        }
    }
}
