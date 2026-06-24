package com.pumpkiiings.pkanticheat.checks.combat;

import com.pumpkiiings.pkanticheat.checks.Check;
import com.pumpkiiings.pkanticheat.data.PlayerData;
import com.pumpkiiings.pkanticheat.data.PlayerDataManager;
import com.pumpkiiings.pkanticheat.events.PlayerPacketEvent;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * GCDCheck — improved with Intave's real Euclidean GCD algorithm.
 *
 * Minecraft maps mouse movement to degrees using:
 *   sens * 0.6 + 0.2  →  (result)^3 * 8  → final step multiplier
 *
 * This means all real rotation deltas must be divisible by a fixed GCD
 * that depends on the player's sensitivity setting.
 *
 * Strategy (from RotationSensitivityHeuristic):
 *   We run an iterative modulo loop to find the GCD between consecutive pitch
 *   deltas. If the GCD keeps changing by > 0.001 every tick *while attacking*,
 *   the rotations are synthetic (not from a real mouse).
 */
public class GCDCheck extends Check {

    public GCDCheck() {
        super("GCD/Aim");
    }

    @SubscribeEvent
    public void onPacket(PlayerPacketEvent event) {
        if (event.getDirection() != PlayerPacketEvent.PacketDirection.INBOUND) return;
        if (!(event.getPacket() instanceof ServerboundMovePlayerPacket rotPacket)) return;

        ServerPlayer player = event.getPlayer();
        PlayerData data = PlayerDataManager.getData(player);

        if (!rotPacket.hasRotation() || player.isCreative() || player.isSpectator()) return;

        float yaw   = rotPacket.getYRot(data.getLastYaw());
        float pitch = rotPacket.getXRot(data.getLastPitch());

        float deltaYaw   = Math.abs(yaw   - data.getLastYaw());
        float deltaPitch = Math.abs(pitch - data.getLastPitch());

        // ── Heuristic 1: Euclidean GCD instability (Intave's RotationSensitivityHeuristic) ──
        // Only run within 200ms of last attack (when aimbots are actively targeting)
        long now = System.currentTimeMillis();
        if (now - data.lastAttackTimestamp < 200L && deltaPitch > 0.0f) {
            float prevGCD = data.prevPitchGCD;
            if (prevGCD == 0f) prevGCD = deltaPitch;

            double a = prevGCD;
            double b = deltaPitch;
            double r;
            int countdown = 100;
            while ((r = a % b) > Math.max(a, b) * 1e-3) {
                a = b;
                b = r;
                if (countdown-- < 0) break;
            }

            float pitchGCD = (float) b;
            double gcdDiff = Math.abs(pitchGCD - prevGCD);
            data.prevPitchGCD = pitchGCD;

            if (gcdDiff > 0.001) {
                if (deltaPitch > 1.0f) {
                    data.gcdSensVL += deltaPitch > 5f ? 10 : 5;
                }
                // Flag every 400 VL accumulated (mirrors Intave's threshold)
                if (data.gcdSensVL > 0 && ((int) Math.round(data.gcdSensVL / 2.0) % 50) == 0) {
                    if (data.gcdSensVL >= 400) {
                        fail(player, "Rotaciones fuera de sync (GCD Sensitivity). VL=" + data.gcdSensVL);
                        data.gcdSensVL = 300; // partial reset
                    }
                }
            } else if (data.gcdSensVL > 0) {
                data.gcdSensVL--;
            }
        } else {
            // Decay outside attack window
            if (data.gcdSensVL > 0) data.gcdSensVL = Math.max(0, data.gcdSensVL - 2);
        }

        // ── Heuristic 2: Aimbot snap (instant >100° yaw + zero pitch change) ──
        if (deltaYaw > 100.0f && deltaPitch == 0.0f) {
            fail(player, "Flick instantáneo antinatural (Aimbot Snap). DeltaYaw=" + String.format("%.1f", deltaYaw));
            event.setCanceled(true);
        }

        // ── Heuristic 3: Micro-rotation (< 0.01° on pitch — physically impossible) ──
        if (deltaPitch > 0.0f && deltaPitch < 0.01f && !player.isPassenger()) {
            data.gcdMicroVL++;
            if (data.gcdMicroVL > 5) {
                fail(player, "Rotación microscópica (GCD Flaw). PitchDelta=" + deltaPitch);
                data.gcdMicroVL = 0;
                event.setCanceled(true);
            }
        } else {
            data.gcdMicroVL = Math.max(0, data.gcdMicroVL - 1);
        }

        data.setLastYaw(yaw);
        data.setLastPitch(pitch);
    }
}
