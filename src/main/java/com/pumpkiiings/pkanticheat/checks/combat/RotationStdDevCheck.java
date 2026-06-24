package com.pumpkiiings.pkanticheat.checks.combat;

import com.pumpkiiings.pkanticheat.checks.Check;
import com.pumpkiiings.pkanticheat.data.PlayerData;
import com.pumpkiiings.pkanticheat.data.PlayerDataManager;
import com.pumpkiiings.pkanticheat.events.PlayerPacketEvent;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * RotationStdDevCheck — ported from Intave's RotationStdDevHeuristic.
 *
 * Killauras look at targets with near-perfect precision tick after tick.
 * A human will jitter ±1–5° around the target because of hand tremor,
 * mouse resolution, and reaction delay. A bot will hover within <0.5° of
 * the perfect look-at vector to the target's center/eye.
 *
 * Strategy:
 *   1. When an attack event fires, we note the target entity.
 *   2. On each rotation packet within 600 ms after an attack, we compute
 *      the "perfect yaw/pitch" to look at the target from the attacker's eye,
 *      then measure the angular error (delta between sent rotation and perfect).
 *   3. Over a sliding window of 15 samples, if StdDev < 0.85° on BOTH axes
 *      we flag the player (bots lock on with superhuman precision).
 */
public class RotationStdDevCheck extends Check {

    private static final int    SAMPLE_SIZE        = 15;
    private static final double STD_DEV_THRESHOLD  = 0.85;
    private static final long   ATTACK_WINDOW_MS   = 600L;
    private static final int    MIN_SAMPLES        = 8;

    public RotationStdDevCheck() {
        super("RotStdDev");
    }

    // ── Track last attacked entity via Forge event ─────────────────────────
    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.isCreative() || player.isSpectator()) return;
        if (!(player instanceof ServerPlayer)) return;

        PlayerData data = PlayerDataManager.getData(player);
        data.lastAttackTimestamp = System.currentTimeMillis();
        data.setLastLookedEntity(event.getTarget());
    }

    // ── Collect rotation samples ───────────────────────────────────────────
    @SubscribeEvent
    public void onPacket(PlayerPacketEvent event) {
        if (event.getDirection() != PlayerPacketEvent.PacketDirection.INBOUND) return;
        if (!(event.getPacket() instanceof ServerboundMovePlayerPacket rotPacket)) return;

        ServerPlayer player = event.getPlayer();
        PlayerData data = PlayerDataManager.getData(player);

        if (player.isCreative() || player.isSpectator()) return;
        if (!rotPacket.hasRotation()) return;

        long now         = System.currentTimeMillis();
        long sinceAttack = now - data.lastAttackTimestamp;

        Entity target = data.getLastLookedEntity();
        if (sinceAttack > ATTACK_WINDOW_MS || target == null || !target.isAlive()) {
            // Decay balance outside window
            if (data.rotStdDevYawBalance   > 0) data.rotStdDevYawBalance   = Math.max(0, data.rotStdDevYawBalance   - 1);
            if (data.rotStdDevPitchBalance > 0) data.rotStdDevPitchBalance = Math.max(0, data.rotStdDevPitchBalance - 1);
            return;
        }

        float sentYaw   = rotPacket.getYRot(data.getLastYaw());
        float sentPitch = rotPacket.getXRot(data.getLastPitch());

        // ── Compute perfect look-at angles ──────────────────────────────────
        Vec3 eyePos    = player.getEyePosition();
        Vec3 targetEye = target.getEyePosition();
        Vec3 diff      = targetEye.subtract(eyePos);

        double horizontalDist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float perfectYaw   = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));
        float perfectPitch = (float) (-Math.toDegrees(Math.atan2(diff.y, horizontalDist)));

        float errorYaw   = Math.abs(normalizeAngle(sentYaw   - perfectYaw));
        float errorPitch = Math.abs(normalizeAngle(sentPitch - perfectPitch));

        // Only count ticks where the player is actually tracking (reasonable error)
        if (errorYaw > 20.0f || errorPitch > 15.0f) return;

        // ── Accumulate samples ──────────────────────────────────────────────
        data.yawToPerfectSamples.addLast(errorYaw);
        data.pitchToPerfectSamples.addLast(errorPitch);

        if (data.yawToPerfectSamples.size()   > SAMPLE_SIZE) data.yawToPerfectSamples.pollFirst();
        if (data.pitchToPerfectSamples.size() > SAMPLE_SIZE) data.pitchToPerfectSamples.pollFirst();

        if (data.yawToPerfectSamples.size() < MIN_SAMPLES) return;

        // ── Compute standard deviation ──────────────────────────────────────
        double stdYaw   = stdDev(data.yawToPerfectSamples);
        double stdPitch = stdDev(data.pitchToPerfectSamples);

        if (stdYaw < STD_DEV_THRESHOLD && stdPitch < STD_DEV_THRESHOLD) {
            data.rotStdDevYawBalance   += 2.0;
            data.rotStdDevPitchBalance += 2.0;

            if (data.rotStdDevYawBalance >= 20.0 && data.rotStdDevPitchBalance >= 20.0) {
                fail(player, String.format(
                    "Precisión de rotación inhumana (RotStdDev Killaura). StdYaw=%.3f° StdPitch=%.3f°",
                    stdYaw, stdPitch
                ));
                data.rotStdDevYawBalance   = 10.0;
                data.rotStdDevPitchBalance = 10.0;
                data.yawToPerfectSamples.clear();
                data.pitchToPerfectSamples.clear();
            }
        } else {
            data.rotStdDevYawBalance   = Math.max(0, data.rotStdDevYawBalance   - 0.5);
            data.rotStdDevPitchBalance = Math.max(0, data.rotStdDevPitchBalance - 0.5);
        }
    }

    private float normalizeAngle(float angle) {
        angle %= 360f;
        if (angle >= 180f)  angle -= 360f;
        if (angle < -180f)  angle += 360f;
        return angle;
    }

    private double stdDev(java.util.ArrayDeque<Float> values) {
        double mean = 0;
        for (float v : values) mean += v;
        mean /= values.size();
        double sumSq = 0;
        for (float v : values) sumSq += (v - mean) * (v - mean);
        return Math.sqrt(sumSq / values.size());
    }
}
