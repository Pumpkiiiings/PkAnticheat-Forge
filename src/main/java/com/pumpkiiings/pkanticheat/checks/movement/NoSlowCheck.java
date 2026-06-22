package com.pumpkiiings.pkanticheat.checks.movement;

import com.pumpkiiings.pkanticheat.checks.Check;

import com.pumpkiiings.pkanticheat.PkConfig;
import com.pumpkiiings.pkanticheat.data.PlayerData;
import com.pumpkiiings.pkanticheat.data.PlayerDataManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class NoSlowCheck extends Check {

    public NoSlowCheck() {
        super("NoSlow");
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide() || player.isCreative() || player.isSpectator()) return;
        
        if (player.isFallFlying() || player.isPassenger()) return;

        PlayerData data = PlayerDataManager.getData(player);
        Vec3 lastPos = data.getLastLocation();
        Vec3 currPos = player.position();

        if (lastPos == Vec3.ZERO) return;

        double deltaX = currPos.x - lastPos.x;
        double deltaZ = currPos.z - lastPos.z;
        double speedSq = deltaX * deltaX + deltaZ * deltaZ;
        
        double maxSpeed = PkConfig.SERVER.noslowMaxSpeed.get();
        
        // If they are using an item (blocking, eating)
        // We only check when they are on the ground, because jumping momentum legitimately carries over in vanilla.
        if (player.isUsingItem() && player.onGround()) {
            if (speedSq > (maxSpeed * maxSpeed)) {
                fail(player, "Moving too fast while using item (" + String.format("%.2f", Math.sqrt(speedSq)) + " blocks/tick)");
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                    sp.connection.teleport(lastPos.x, lastPos.y, lastPos.z, player.getYRot(), player.getXRot());
                }
            }
        }
    }
}
