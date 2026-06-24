package com.pumpkiiings.pkanticheat.data;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerLocationTracker {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide() || player.isSpectator()) return;

        PlayerData data = PlayerDataManager.getData(player);

        // Update Location globally for all checks to use
        data.setCurrentLocation(player.position());
        Vec3 currPos = data.getCurrentLocation();

        // Update Ground State and Safe Location globally
        boolean currentlyOnGround = player.onGround();

        // Only update safe ground location if they are genuinely on ground and not in water/lava
        if (currentlyOnGround && !player.isInWater() && !player.isInLava() && !player.onClimbable()) {
            data.setLastValidGroundLocation(currPos);
        }

        data.setWasOnGround(currentlyOnGround);
    }
}
