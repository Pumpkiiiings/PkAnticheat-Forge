package com.pumpkiiings.pkanticheat.checks.packet;

import com.pumpkiiings.pkanticheat.checks.Check;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class DamageIndicatorSpoofCheck extends Check {

    public DamageIndicatorSpoofCheck() {
        super("DamageIndicatorSpoof");
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketInjector.inject(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketInjector.remove(player);
        }
    }
}
