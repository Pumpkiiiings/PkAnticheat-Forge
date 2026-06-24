package com.pumpkiiings.pkanticheat.checks.combat;

import com.pumpkiiings.pkanticheat.checks.Check;
import com.pumpkiiings.pkanticheat.data.PlayerData;
import com.pumpkiiings.pkanticheat.data.PlayerDataManager;
import com.pumpkiiings.pkanticheat.events.PlayerPacketEvent;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * HitboxAccuracyCheck — ported from Intave's HitboxAccuracyHeuristic.
 *
 * Aimbots aim at the exact center of the target hitbox with superhuman consistency.
 * A human player hits at random positions across the hitbox.
 *
 * For each attack, we compute the angular distance between the player's
 * actual look direction and the perfect look-at angle to the AABB center.
 * Over a rolling window of 20 attacks, if >90% of hits are within 0.5° of
 * perfect center → flag. Guard: only flags after 40+ total attacks.
 */
public class HitboxAccuracyCheck extends Check {

    private static final float  CENTER_THRESHOLD_DEG       = 0.5f;
    private static final int    WINDOW_SIZE                 = 20;
    private static final double FLAG_RATIO                  = 0.90;
    private static final int    MIN_ATTACKS_BEFORE_FLAG     = 40;

    public HitboxAccuracyCheck() {
        super("HitboxAcc");
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.isCreative() || player.isSpectator()) return;
        if (!(player instanceof ServerPlayer)) return;

        PlayerData data = PlayerDataManager.getData(player);
        Entity target = event.getTarget();

        evaluateAttack(player, data, target);
    }

    private void evaluateAttack(Player player, PlayerData data, Entity target) {
        if (target == null || !target.isAlive()) return;

        Vec3 eyePos = player.getEyePosition();

        float yaw   = player.getYRot();
        float pitch = player.getXRot();

        // Target AABB center
        AABB box    = target.getBoundingBox();
        Vec3 center = box.getCenter();

        // Perfect yaw/pitch to AABB center
        Vec3 diff = center.subtract(eyePos);
        double horizDist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float perfectYaw   = (float)  Math.toDegrees(Math.atan2(-diff.x, diff.z));
        float perfectPitch = (float) (-Math.toDegrees(Math.atan2(diff.y, horizDist)));

        float errorYaw   = Math.abs(normalizeAngle(yaw   - perfectYaw));
        float errorPitch = Math.abs(normalizeAngle(pitch - perfectPitch));
        float totalError = (float) Math.sqrt(errorYaw * errorYaw + errorPitch * errorPitch);

        data.hitboxAccuracyAttacks++;

        float centered = totalError < CENTER_THRESHOLD_DEG ? 1.0f : 0.0f;
        data.distToPerfectYawList.addLast(centered);
        if (data.distToPerfectYawList.size() > WINDOW_SIZE) data.distToPerfectYawList.pollFirst();

        // Track yaw speed at attack moment
        float yawSpeed = Math.abs(normalizeAngle(yaw - data.getLastYaw()));
        data.yawSpeedList.addLast(yawSpeed);
        if (data.yawSpeedList.size() > WINDOW_SIZE) data.yawSpeedList.pollFirst();

        // ── Evaluate ratio ──────────────────────────────────────────────────
        if (data.hitboxAccuracyAttacks < MIN_ATTACKS_BEFORE_FLAG) return;
        if (data.distToPerfectYawList.size() < WINDOW_SIZE) return;

        long centeredCount = data.distToPerfectYawList.stream().filter(v -> v >= 1.0f).count();
        double ratio = (double) centeredCount / WINDOW_SIZE;

        if (ratio >= FLAG_RATIO) {
            double avgYawSpeed = data.yawSpeedList.stream()
                .mapToDouble(Float::doubleValue).average().orElse(999.0);

            data.hitboxAccuracyVL += 3.0;
            if (data.hitboxAccuracyVL >= 15.0) {
                fail(player, String.format(
                    "Precisión de hitbox inhumana (Aimbot/Hitbox). Ratio=%.0f%% AvgYawSpd=%.2f°",
                    ratio * 100, avgYawSpeed
                ));
                data.hitboxAccuracyVL = 8.0;
                data.distToPerfectYawList.clear();
            }
        } else {
            data.hitboxAccuracyVL = Math.max(0, data.hitboxAccuracyVL - 1.0);
        }
    }

    private float normalizeAngle(float angle) {
        angle %= 360f;
        if (angle >= 180f)  angle -= 360f;
        if (angle < -180f)  angle += 360f;
        return angle;
    }
}
