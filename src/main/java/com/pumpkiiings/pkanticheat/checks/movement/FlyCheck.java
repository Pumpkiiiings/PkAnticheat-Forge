package com.pumpkiiings.pkanticheat.checks.movement;

import com.pumpkiiings.pkanticheat.checks.Check;

import com.pumpkiiings.pkanticheat.data.PlayerData;
import com.pumpkiiings.pkanticheat.data.PlayerDataManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class FlyCheck extends Check {

    public FlyCheck() {
        super("Fly");
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide() || player.isCreative() || player.isSpectator()) return;

        // Ignore cases
        if (player.isFallFlying() || player.isPassenger() || player.isInWater() || player.isInLava() || player.onClimbable()) {
            return;
        }

        PlayerData data = PlayerDataManager.getData(player);
        Vec3 lastPos = data.getLastLocation();
        Vec3 currPos = data.getCurrentLocation();

        if (lastPos == Vec3.ZERO) return;

        double deltaY = currPos.y - lastPos.y;
        boolean currentlyOnGround = player.onGround();
        boolean previouslyOnGround = data.wasOnGround();

        if (!currentlyOnGround && !previouslyOnGround) {
            Vec3 velocity = player.getDeltaMovement();
            if (velocity.y == 0.0 && !player.isInWater() && !player.onClimbable()) {
                data.flyVL++;
                
                if (data.flyVL > 3) {
                    fail(player, "Hovering in air (Fly)");
                    
                    // Setback logic
                    Vec3 safePos = data.getLastValidGroundLocation();
                    if (safePos != Vec3.ZERO) {
                        ((ServerPlayer) player).connection.teleport(safePos.x, safePos.y, safePos.z, player.getYRot(), player.getXRot());
                    }
                }
            } else {
                data.flyVL = 0;
            }
        } else if (currentlyOnGround) {
            data.flyVL = 0;
        }
    }
}
