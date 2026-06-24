package com.pumpkiiings.pkanticheat.checks.combat;

import com.pumpkiiings.pkanticheat.checks.Check;
import com.pumpkiiings.pkanticheat.data.PlayerData;
import com.pumpkiiings.pkanticheat.data.PlayerDataManager;
import com.pumpkiiings.pkanticheat.events.PlayerPacketEvent;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * RotationSnapCheck — ported from Intave's RotationSnapHeuristic.
 *
 * Killauras and AimAssist mods frequently produce a characteristic "snap":
 * the yaw/pitch changes sharply in the exact tick an attack is sent, then
 * returns to the player's prior angle or continues linearly.
 *
 * A human player: their rotation delta is smooth across several ticks before
 * and after the attack. They don't suddenly change angular velocity exactly
 * at the attack moment.
 *
 * Detection strategy (two-tier):
 *
 * Tier 1 — Correlated Snap:
 *   If the yaw delta in the tick containing an attack is ≥2.5× the previous
 *   tick's delta, there's a sudden acceleration correlated with the attack.
 *
 * Tier 2 — Instant Reversal:
 *   After a snap, if yaw direction reverses significantly, it means the client
 *   sent a fake look packet purely to satisfy the hitbox check, then snapped back.
 */
public class RotationSnapCheck extends Check {

    private static final double SNAP_MULTIPLIER   = 2.5;
    private static final double MIN_DELTA         = 1.5;
    private static final int    FLAG_VL           = 6;

    public RotationSnapCheck() {
        super("RotSnap");
    }

    // ── Track attack timestamp via Forge event ─────────────────────────────
    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.isCreative() || player.isSpectator()) return;
        if (!(player instanceof ServerPlayer)) return;

        PlayerData data = PlayerDataManager.getData(player);
        data.lastAttackTimestamp = System.currentTimeMillis();
    }

    // ── Process rotation packets ───────────────────────────────────────────
    @SubscribeEvent
    public void onPacket(PlayerPacketEvent event) {
        if (event.getDirection() != PlayerPacketEvent.PacketDirection.INBOUND) return;
        if (!(event.getPacket() instanceof ServerboundMovePlayerPacket rotPacket)) return;

        ServerPlayer player = event.getPlayer();
        PlayerData data = PlayerDataManager.getData(player);

        if (player.isCreative() || player.isSpectator()) return;
        if (!rotPacket.hasRotation()) return;

        float yaw   = rotPacket.getYRot(data.getLastYaw());
        float pitch = rotPacket.getXRot(data.getLastPitch());

        double deltaYaw = Math.abs(yaw - data.getLastYaw());

        double prevDelta = data.prevTickYawDelta;

        long sinceAttack    = System.currentTimeMillis() - data.lastAttackTimestamp;
        boolean inAttackWin = sinceAttack < 100L;

        // ── Tier 1: Correlated snap ─────────────────────────────────────────
        if (inAttackWin && deltaYaw >= MIN_DELTA && prevDelta > 0.1) {
            double ratio = deltaYaw / prevDelta;
            if (ratio >= SNAP_MULTIPLIER) {
                data.rotationSnapVL += 3;
            }
        }

        // ── Tier 2: Instant reversal (snap + snap-back) ─────────────────────
        if (sinceAttack < 200L && deltaYaw >= MIN_DELTA && prevDelta >= MIN_DELTA) {
            float rawSigned  = yaw - data.getLastYaw();
            // Detect sign reversal with significant magnitude
            boolean reversed = (rawSigned > 0) != (data.prevTickYawDelta > 0)
                               && Math.abs(rawSigned) > 5.0;
            if (reversed) {
                data.rotationSnapVL += 5;
            }
        }

        // ── Evaluate and flag ───────────────────────────────────────────────
        if (data.rotationSnapVL >= FLAG_VL) {
            fail(player, String.format(
                "Snap de rotación correlacionado con ataque (Killaura/AimAssist). ΔYaw=%.1f° prevΔ=%.1f°",
                deltaYaw, prevDelta
            ));
            data.rotationSnapVL = Math.max(0, data.rotationSnapVL - 4);
        }

        // Natural decay outside attack window
        if (data.rotationSnapVL > 0 && !inAttackWin) {
            data.rotationSnapVL = Math.max(0, data.rotationSnapVL - 1);
        }

        // Update rolling state
        data.prevTickYawDelta = deltaYaw;
    }
}
