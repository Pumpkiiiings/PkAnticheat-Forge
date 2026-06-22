package com.pumpkiiings.pkanticheat.checks.movement;

import com.pumpkiiings.pkanticheat.checks.Check;
import com.pumpkiiings.pkanticheat.data.PlayerData;
import com.pumpkiiings.pkanticheat.data.PlayerDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class WaterWalkCheck extends Check {

    public WaterWalkCheck() {
        super("WaterWalk");
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        if (event.player.level().isClientSide() || event.player.isCreative() || event.player.isFallFlying()) return;

        PlayerData data = PlayerDataManager.getData(event.player);
        Vec3 curr = event.player.position();
        Vec3 last = data.getLastLocation();
        Vec3 velocity = event.player.getDeltaMovement();

        if (last != Vec3.ZERO && !event.player.onGround() && !event.player.isInWater()) {
            double deltaY = curr.y - last.y;
            
            BlockPos below = BlockPos.containing(curr.x, curr.y - 0.1, curr.z);
            BlockState stateBelow = event.player.level().getBlockState(below);
            boolean isHoveringOverWater = stateBelow.getBlock() instanceof LiquidBlock;

            if (isHoveringOverWater && velocity.y <= 0 && event.player.fallDistance == 0) {
                data.waterWalkVL++;
                
                if (data.waterWalkVL > 4) {
                    fail(event.player, "Walking on water (Jesus)");
                    
                    // Force drop them into the water AND cancel all horizontal movement
                    event.player.setDeltaMovement(new Vec3(0, -0.5, 0));
                    event.player.hurtMarked = true;
                }
            } else {
                data.waterWalkVL = 0;
            }
        }
    }
}
