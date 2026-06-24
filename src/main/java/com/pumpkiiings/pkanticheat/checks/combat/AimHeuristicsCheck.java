package com.pumpkiiings.pkanticheat.checks.combat;

import com.pumpkiiings.pkanticheat.checks.Check;
import com.pumpkiiings.pkanticheat.data.PlayerData;
import com.pumpkiiings.pkanticheat.data.PlayerDataManager;
import com.pumpkiiings.pkanticheat.events.PlayerPacketEvent;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.Deque;

public class AimHeuristicsCheck extends Check {

    // How many packets of history to keep for variance analysis
    private static final int HISTORY_SIZE = 20;

    public AimHeuristicsCheck() {
        super("Aim");
    }

    @SubscribeEvent
    public void onPacket(PlayerPacketEvent event) {
        if (event.getDirection() != PlayerPacketEvent.PacketDirection.INBOUND) return;
        if (!(event.getPacket() instanceof ServerboundMovePlayerPacket rotPacket)) return;

        ServerPlayer player = event.getPlayer();
        PlayerData data = PlayerDataManager.getData(player);

        if (!rotPacket.hasRotation()) return;

        float yaw   = rotPacket.getYRot(data.getLastYaw());
        float pitch = rotPacket.getXRot(data.getLastPitch());

        float deltaYaw   = Math.abs(yaw   - data.getLastYaw());
        float deltaPitch = Math.abs(pitch - data.getLastPitch());

        // ----------------------------------------------------------------
        // Heuristic A: Variance analysis
        // Real mice produce irregular deltas. Bots/aimbots produce patterns
        // with zero (or near-zero) variance on one axis while swinging the other.
        // We track the last HISTORY_SIZE yaw/pitch deltas and compute variance.
        // ----------------------------------------------------------------
        Deque<Float> yawHistory   = getOrCreateQueue(data, "yaw");
        Deque<Float> pitchHistory = getOrCreateQueue(data, "pitch");

        yawHistory.addLast(deltaYaw);
        pitchHistory.addLast(deltaPitch);

        if (yawHistory.size() > HISTORY_SIZE)   yawHistory.pollFirst();
        if (pitchHistory.size() > HISTORY_SIZE) pitchHistory.pollFirst();

        if (yawHistory.size() == HISTORY_SIZE && pitchHistory.size() == HISTORY_SIZE) {
            double varYaw   = variance(yawHistory);
            double varPitch = variance(pitchHistory);

            // Heuristic A1 – Perfect pitch lock while yaw swings (common in Forgebot / Baritone combat)
            if (varPitch < 0.0001 && varYaw > 2.0 && deltaPitch == 0.0f) {
                data.pitchDeltas.add(0f);
                if (data.pitchDeltas.size() > 5) {
                    fail(player, "Pitch bloqueado con Yaw activo (Aimbot). VarPitch=" + String.format("%.6f", varPitch));
                    data.pitchDeltas.clear();
                }
            } else {
                data.pitchDeltas.clear();
            }

            // Heuristic A2 – Totally flat deltas on BOTH axes for several ticks while in combat
            // (AutoAim holding a fixed angle perfectly)
            if (varYaw < 0.0001 && varPitch < 0.0001 && deltaYaw == 0.0f && deltaPitch == 0.0f) {
                // Ignore AFK players (no movement)
                boolean isMoving = player.getDeltaMovement().horizontalDistanceSqr() > 0.001;
                if (isMoving) {
                    data.yawDeltas.add(0f);
                    if (data.yawDeltas.size() > 8) {
                        fail(player, "Rotación completamente fija mientras se mueve (AutoAim).");
                        data.yawDeltas.clear();
                    }
                }
            } else {
                data.yawDeltas.clear();
            }
        }

        // ----------------------------------------------------------------
        // Heuristic B: Cinematic-style rotation smoothness check
        // Real mouse movement has different speeds per tick (acceleration curve).
        // An aimbot targeting a moving player locks with near-constant angular speed.
        // If we detect 10+ consecutive ticks of almost exactly the same deltaYaw,
        // it's a bot tracking a moving target at a constant angular velocity.
        // ----------------------------------------------------------------
        float lastDeltaYaw = data.getNetworkYaw(); // reusing this field to store last packet's deltaYaw
        if (lastDeltaYaw > 0.5f && deltaYaw > 0.5f) {
            float ratio = Math.max(lastDeltaYaw, deltaYaw) / Math.min(lastDeltaYaw, deltaYaw);
            if (ratio < 1.05f) { // Less than 5% variation between consecutive ticks
                data.pitchDeltas.add(deltaYaw);
                if (data.pitchDeltas.size() > 10) {
                    fail(player, "Seguimiento angular constante (Smooth Aimbot). Ratio=" + String.format("%.3f", ratio));
                    data.pitchDeltas.clear();
                }
            } else {
                data.pitchDeltas.clear();
            }
        } else {
            data.pitchDeltas.clear();
        }

        // Store delta as "last" for next tick's Heuristic B
        data.setNetworkYaw(deltaYaw);
        data.setNetworkPitch(deltaPitch);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Deque<Float> getOrCreateQueue(PlayerData data, String axis) {
        // Reuse existing queues stored in PlayerData
        // yawDeltas  -> used for yaw history here
        // pitchDeltas -> used for pitch history here
        if (axis.equals("yaw"))   return (Deque<Float>) data.yawDeltas;
        return (Deque<Float>) data.pitchDeltas;
    }

    private double variance(Deque<Float> values) {
        double mean = 0;
        for (float v : values) mean += v;
        mean /= values.size();
        double sumSq = 0;
        for (float v : values) sumSq += (v - mean) * (v - mean);
        return sumSq / values.size();
    }
}
