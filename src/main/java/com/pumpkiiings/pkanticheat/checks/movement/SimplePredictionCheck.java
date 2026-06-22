package com.pumpkiiings.pkanticheat.checks.movement;

import com.pumpkiiings.pkanticheat.checks.Check;
import com.pumpkiiings.pkanticheat.data.PlayerData;
import com.pumpkiiings.pkanticheat.data.PlayerDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SimplePredictionCheck extends Check {

    public SimplePredictionCheck() {
        super("Prediction");
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        if (event.player.level().isClientSide() || event.player.isCreative() || event.player.isFallFlying()) return;
        if (event.player.isPassenger() || !(event.player instanceof ServerPlayer serverPlayer)) return;

        PlayerData data = PlayerDataManager.getData(serverPlayer);
        data.updatePing(serverPlayer.latency);

        Vec3 curr = event.player.position();
        Vec3 last = data.getLastLocation();

        if (last != Vec3.ZERO) {
            double deltaX = curr.x - last.x;
            double deltaZ = curr.z - last.z;
            double distanceSq = deltaX * deltaX + deltaZ * deltaZ;

            // Speed potion effect (+20% base speed per level)
            int speedLevel = 0;
            if (event.player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                speedLevel = event.player.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() + 1;
            }
            double speedMultiplier = 1.0 + (0.2 * speedLevel);
            
            // Maximum theoretical horizontal speed calculation:
            // Base walk limit: ~0.35 blocks/tick (includes some sprinting momentum)
            double maxExpectedDistance = 0.35; 

            if (event.player.isSprinting()) {
                // A perfect sprint jump in vanilla can hit ~0.61 blocks/tick max velocity
                maxExpectedDistance = 0.65; 
            }

            maxExpectedDistance *= speedMultiplier;

            // Ice and Slime modifiers
            // A simple simulator adds a buffer to cover the lack of block-friction tracking
            double baseLagBuffer = 0.30; 
            
            // Dynamic Ping & Jitter Compensation
            double pingCompensation = (data.currentPing / 1000.0) * 0.5; // Up to 0.5 blocks extra for 1000ms ping
            if (data.pingJitter > 100) {
                pingCompensation += 0.4; // Major lag spike
            }
            
            double maxAllowedDistance = maxExpectedDistance + baseLagBuffer + pingCompensation;

            // If taking velocity (knockback), we must allow more movement
            if (data.getVelocityTicks() > 0) {
                Vec3 expectedVel = data.getExpectedVelocity();
                double velDist = Math.sqrt(expectedVel.x * expectedVel.x + expectedVel.z * expectedVel.z);
                maxAllowedDistance += velDist;
            }

            double maxAllowedSq = maxAllowedDistance * maxAllowedDistance;

            if (distanceSq > maxAllowedSq) {
                double distance = Math.sqrt(distanceSq);
                fail(event.player, "Exceeded maximum possible speed (Bounds Predictor)");
                
                // Brutal Setback
                if (event.player instanceof ServerPlayer sp) {
                    Vec3 safePos = data.getLastValidGroundLocation();
                    if (safePos != Vec3.ZERO) {
                        sp.connection.teleport(safePos.x, safePos.y, safePos.z, sp.getYRot(), sp.getXRot());
                    } else {
                        sp.connection.teleport(last.x, last.y, last.z, sp.getYRot(), sp.getXRot());
                    }
                }
            }
        }
    }
}
