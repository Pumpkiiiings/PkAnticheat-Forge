package com.pumpkiiings.pkanticheat.checks.movement;

import com.pumpkiiings.pkanticheat.checks.Check;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SafeWalkCheck extends Check {

    public SafeWalkCheck() {
        super("SafeWalk");
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        if (event.player.level().isClientSide() || event.player.isCreative() || event.player.isFallFlying()) return;

    }
}
