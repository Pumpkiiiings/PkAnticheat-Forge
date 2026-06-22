package com.pumpkiiings.pkanticheat.checks.movement;

import com.pumpkiiings.pkanticheat.checks.Check;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AntiVoidCheck extends Check {

    public AntiVoidCheck() {
        super("AntiVoid");
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        if (event.player.level().isClientSide() || event.player.isCreative() || event.player.isFallFlying()) return;

        double minHeight = event.player.level().getMinBuildHeight();
        
        // If they are deep in the void (e.g. -65)
        if (event.player.getY() < minHeight - 1.0) {
            double deltaY = event.player.getDeltaMovement().y;
            
            // If they are not falling quickly or are hovering/ascending
            if (deltaY > -0.1) {
                fail(event.player, "Stopping fall in the void");
                // Force them to drop to their death
                event.player.setDeltaMovement(event.player.getDeltaMovement().x, -3.0, event.player.getDeltaMovement().z);
                event.player.hurtMarked = true;
            }
        }
    }
}
