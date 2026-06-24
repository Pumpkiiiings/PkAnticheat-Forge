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
 * PacketOrderCheck — ported from Intave's PacketOrderSwingHeuristic.
 *
 * In vanilla, the swing packet (ARM_ANIMATION) always arrives in the same
 * tick as the attack, and critically the swing comes FIRST in the packet
 * stream (animation → interaction). AutoBlock / ComboBots often send the
 * attack BEFORE the swing in the same tick window.
 *
 * We use:
 *   - ServerboundSwingPacket → set swingBeforeAttack = true
 *   - AttackEntityEvent      → check if swingBeforeAttack was set already
 *   - ServerboundMovePlayerPacket → reset state (tick boundary)
 */
public class PacketOrderCheck extends Check {

    public PacketOrderCheck() {
        super("PacketOrder");
    }

    // ── Forge high-level attack event ──────────────────────────────────────
    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.isCreative() || player.isSpectator()) return;
        if (!(player instanceof ServerPlayer)) return;

        PlayerData data = PlayerDataManager.getData(player);

        // Attack arrived but NO swing preceded it → wrong packet order
        if (!data.swingBeforeAttack) {
            data.packetOrderVL++;
            if (data.packetOrderVL >= 3) {
                fail(player, "Orden de paquetes incorrecto: ataque sin swing previo (AutoBlock/Bot)");
                event.setCanceled(true);
            }
        } else {
            data.packetOrderVL = Math.max(0, data.packetOrderVL - 1);
        }
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
            // Swing received — mark it before any potential attack this tick
            data.swingBeforeAttack = true;

        } else if (packet instanceof ServerboundMovePlayerPacket) {
            // Tick boundary — reset order tracking
            data.swingBeforeAttack = false;
        }
    }
}
