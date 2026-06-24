package com.pumpkiiings.pkanticheat.checks.combat;

import com.pumpkiiings.pkanticheat.checks.Check;
import com.pumpkiiings.pkanticheat.data.PlayerData;
import com.pumpkiiings.pkanticheat.data.PlayerDataManager;
import com.pumpkiiings.pkanticheat.events.PlayerPacketEvent;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * NoSwingCheck — ported from Intave's NoSwingHeuristic.
 *
 * In vanilla, the client always sends an ARM_ANIMATION (swing) packet in the
 * same tick as an attack. Killauras (and other combat bots) frequently omit
 * the swing packet to perform "silent attacks".
 *
 * Logic (per-tick):
 *   - When we receive a ServerboundSwingPacket  → mark swingThisTick = true
 *   - When AttackEntityEvent fires              → mark attackThisTick = true
 *   - On the next ServerboundMovePlayerPacket (tick boundary) we evaluate:
 *       attack with NO swing → flag
 */
public class NoSwingCheck extends Check {

    public NoSwingCheck() {
        super("NoSwing");
    }

    // ── Forge high-level attack event ──────────────────────────────────────
    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.isCreative() || player.isSpectator()) return;
        if (!(player instanceof ServerPlayer)) return;

        PlayerData data = PlayerDataManager.getData(player);
        data.attackedThisTick = true;
    }

    // ── Low-level packet listener (swing + tick boundary) ─────────────────
    @SubscribeEvent
    public void onPacket(PlayerPacketEvent event) {
        if (event.getDirection() != PlayerPacketEvent.PacketDirection.INBOUND) return;

        ServerPlayer player = event.getPlayer();
        PlayerData data = PlayerDataManager.getData(player);

        if (player.isCreative() || player.isSpectator()) return;

        Object packet = event.getPacket();

        if (packet instanceof ServerboundSwingPacket) {
            data.swungThisTick = true;

        } else if (packet instanceof ServerboundMovePlayerPacket) {
            // Tick boundary — evaluate the previous tick's state
            if (data.attackedThisTick && !data.swungThisTick) {
                data.noSwingVL++;
                if (data.noSwingVL >= 2) {
                    fail(player, "Ataque sin animación de brazo (NoSwing / Silent Killaura)");
                }
            } else if (data.swungThisTick) {
                data.noSwingVL = Math.max(0, data.noSwingVL - 1);
            }

            // Reset per-tick state
            data.swungThisTick    = false;
            data.attackedThisTick = false;
        }
    }
}
