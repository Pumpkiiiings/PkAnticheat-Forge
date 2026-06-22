package com.pumpkiiings.pkanticheat.checks.movement;

import com.pumpkiiings.pkanticheat.checks.Check;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import com.pumpkiiings.pkanticheat.data.PlayerData;
import com.pumpkiiings.pkanticheat.data.PlayerDataManager;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class NoFallCheck extends Check {

    public NoFallCheck() {
        super("NoFall");
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        if (event.player.level().isClientSide() || event.player.isCreative() || event.player.isFallFlying()) return;

        PlayerData data = PlayerDataManager.getData(event.player);
        Vec3 curr = event.player.position();
        Vec3 last = data.getLastLocation();
        
        // In Forge, player.onGround() is largely controlled by the client.
        // We check the blocks directly below them to simulate true server side logic.
        BlockPos below = BlockPos.containing(event.player.getX(), event.player.getY() - 0.1, event.player.getZ());
        BlockState stateBelow = event.player.level().getBlockState(below);
        
        boolean isClientOnGround = event.player.onGround();
        
        // Track actual fall distance mathematically
        if (last != Vec3.ZERO && curr.y < last.y && stateBelow.isAir()) {
            data.realFallDistance += (last.y - curr.y);
        } else if (!stateBelow.isAir()) {
            data.realFallDistance = 0.0f; // Reset when actually touching ground
        }
        
        // If they claim to be on the ground, but they are clearly falling through air
        if (isClientOnGround && stateBelow.isAir() && data.realFallDistance > 1.5f) {
            data.noFallVL++;
            
            if (data.noFallVL > 2) {
                fail(event.player, "Spoofed onGround packet (NoFall)");
                
                // Force damage based on true fall distance
                if (data.realFallDistance > 3.0f) {
                    event.player.hurt(event.player.damageSources().fall(), (float) (data.realFallDistance - 3.0f));
                    data.realFallDistance = 0.0f; // Only damage once
                }
            }
        } else if (!stateBelow.isAir()) {
            data.noFallVL = 0;
        }
    }
}
