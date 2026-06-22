package com.pumpkiiings.pkanticheat.checks.combat;

import com.pumpkiiings.pkanticheat.checks.Check;
import com.pumpkiiings.pkanticheat.data.PlayerData;
import com.pumpkiiings.pkanticheat.data.PlayerDataManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class VelocityCheck extends Check {

    public VelocityCheck() {
        super("Velocity");
    }

    @SubscribeEvent
    public void onKnockback(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (player.level().isClientSide() || player.isCreative()) return;

            PlayerData data = PlayerDataManager.getData(player);
            
            // Calculate expected velocity vector based on strength and ratio
            double strength = event.getStrength();
            double ratioX = event.getRatioX();
            double ratioZ = event.getRatioZ();
            
            // Simplified Vanilla Knockback math approximation for Y axis
            double expectedY = 0.4; 
            
            data.setExpectedVelocity(new Vec3(ratioX * strength, expectedY, ratioZ * strength));
            data.setVelocityTicks(5); // Check over the next 5 ticks
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        
        Player player = event.player;
        if (player.level().isClientSide()) return;

        PlayerData data = PlayerDataManager.getData(player);
        
        if (data.getVelocityTicks() > 0) {
            data.setVelocityTicks(data.getVelocityTicks() - 1);
            
            Vec3 expected = data.getExpectedVelocity();
            if (expected != Vec3.ZERO) {
                // If they are taking knockback, their delta Y should increase.
                // We check if their Y velocity completely ignored the knockback (deltaY <= 0)
                // With a 20% margin or ping consideration, we just check if they didn't go up at all.
                Vec3 currentPos = player.position();
                Vec3 lastPos = data.getLastLocation();
                
                if (lastPos != Vec3.ZERO) {
                    double deltaY = currentPos.y - lastPos.y;
                    
                    // In the tick right after knockback, deltaY should be > 0.
                    // If after 5 ticks they never moved up, and they weren't blocked by a ceiling...
                    // (A simple heuristic to prevent false positives with lag)
                    if (data.getVelocityTicks() == 0) {
                        if (deltaY <= 0 && !player.onGround() && !player.isInWater()) {
                            // They took horizontal/vertical knockback but didn't move vertically at all -> Velocity!
                            // Allowing a generous margin: just checking if they resisted the upward lift entirely.
                            fail(player, "Ignored vertical knockback (Anti-Knockback)");
                            data.setExpectedVelocity(Vec3.ZERO);
                        }
                    }
                }
            }
        }
    }
}
