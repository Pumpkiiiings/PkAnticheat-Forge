package com.pumpkiiings.pkanticheat.checks.movement;

import com.pumpkiiings.pkanticheat.checks.Check;
import com.pumpkiiings.pkanticheat.data.PlayerData;
import com.pumpkiiings.pkanticheat.data.PlayerDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SpiderCheck extends Check {

    public SpiderCheck() {
        super("Spider");
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        if (event.player.level().isClientSide() || event.player.isCreative() || event.player.isFallFlying()) return;

        PlayerData data = PlayerDataManager.getData(event.player);
        Vec3 curr = event.player.position();
        Vec3 last = data.getLastLocation();
        Vec3 velocity = event.player.getDeltaMovement();

        if (event.player.horizontalCollision && velocity.y > 0.0) {
            data.spiderVL++;
            
            if (data.spiderVL > 3) {
                fail(event.player, "Climbing wall without ladder (Spider)");

                // Force them to fall down
                event.player.setDeltaMovement(new Vec3(velocity.x, -0.5, velocity.z));
                event.player.hurtMarked = true; // Sync velocity to client
            }
        } else if (event.player.onGround() || event.player.onClimbable() || velocity.y <= 0.0) {
            data.spiderVL = 0;
        }
    }
}
