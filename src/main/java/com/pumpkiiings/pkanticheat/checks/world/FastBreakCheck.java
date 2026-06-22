package com.pumpkiiings.pkanticheat.checks.world;

import com.pumpkiiings.pkanticheat.checks.Check;

import com.pumpkiiings.pkanticheat.data.PlayerData;
import com.pumpkiiings.pkanticheat.data.PlayerDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class FastBreakCheck extends Check {

    public FastBreakCheck() {
        super("FastBreak");
    }

    @SubscribeEvent
    public void onBlockInteract(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.isCreative()) return;

        PlayerData data = PlayerDataManager.getData(player);
        data.setMiningBlock(event.getPos());
        data.setMiningStartTime(System.currentTimeMillis());
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide() || player.isCreative()) return;

        PlayerData data = PlayerDataManager.getData(player);
        BlockPos pos = event.getPos();

        if (pos.equals(data.getMiningBlock())) {
            long timeElapsed = System.currentTimeMillis() - data.getMiningStartTime();
            BlockState state = event.getState();
            float destroySpeedPerTick = state.getDestroyProgress(player, player.level(), pos);
            
            // If the block does not break instantly in vanilla (e.g., no Efficiency 5 + Haste 2 on stone)
            if (destroySpeedPerTick < 1.0f && destroySpeedPerTick > 0.0f) {
                int expectedTicks = (int) Math.ceil(1.0f / destroySpeedPerTick);
                long expectedTimeMs = expectedTicks * 50L;
                
                // Allow a 30% margin of error due to ping, tick drift, and client side prediction
                long minimumAllowedTime = (long) (expectedTimeMs * 0.7) - 50L;

                if (timeElapsed < minimumAllowedTime) {
                    fail(player, "FastBreak (" + timeElapsed + "ms < " + minimumAllowedTime + "ms)");
                    event.setCanceled(true);
                }
            }
        }
    }
}
