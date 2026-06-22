package com.pumpkiiings.pkanticheat.checks.packet;

import com.pumpkiiings.pkanticheat.AlertManager;
import com.pumpkiiings.pkanticheat.checks.Check;
import com.pumpkiiings.pkanticheat.data.PlayerData;
import com.pumpkiiings.pkanticheat.data.PlayerDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class TimerCheck extends Check {

    public TimerCheck() {
        super("Timer");
    }

    private static final TimerCheck INSTANCE = new TimerCheck();

    public static void handle(ServerPlayer player) {
        if (player.level().isClientSide()) return;
        
        PlayerData data = PlayerDataManager.getData(player);
        long now = System.currentTimeMillis();
        long lastTime = data.lastMovementPacketTime;

        if (lastTime == 0) {
            data.lastMovementPacketTime = now;
            return;
        }

        long delay = now - lastTime;

        // Expected delay between movement packets is 50ms (20 TPS).
        // If they send packets faster (e.g. every 40ms), balance increases (+10ms per packet).
        // If they lag, delay is large (e.g. 500ms), balance drops heavily.
        data.timerBalance += (50 - delay);

        // Clamp the balance at a reasonable minimum to prevent lag spikes from giving them a huge negative balance
        if (data.timerBalance < -500) {
            data.timerBalance = -500;
        }

        // Slow Timer detection: if they consistently send packets slower than 0.6x speed (80ms+ delay)
        if (delay > 80) {
            data.slowTimerVL++;
            if (data.slowTimerVL > 10) { // Slower than normal for ~1 second
                INSTANCE.fail(player, "Moving too slow (Slow Timer/Blink)");
                
                // Force a drop or setback so they can't hover in mid-air with Timer 0.1
                Vec3 safePos = data.getLastValidGroundLocation();
                if (safePos != Vec3.ZERO) {
                    player.connection.teleport(safePos.x, safePos.y, safePos.z, player.getYRot(), player.getXRot());
                }
                data.slowTimerVL = 0;
            }
        } else {
            // If they send a normal packet or fast packet, reset the slow VL to avoid false flagging lag spikes
            if (delay < 60) {
                data.slowTimerVL = 0;
            }
        }

        // 250ms of positive balance means they have sent 5 extra ticks into the future.
        if (data.timerBalance > 250) {
            INSTANCE.fail(player, "Moving too fast (Timer)");
            
            // Setback logic
            Vec3 safePos = data.getLastValidGroundLocation();
            if (safePos != Vec3.ZERO) {
                player.connection.teleport(safePos.x, safePos.y, safePos.z, player.getYRot(), player.getXRot());
            }
            
            // Reset balance after flag
            data.timerBalance = 0;
        }
    }
}
