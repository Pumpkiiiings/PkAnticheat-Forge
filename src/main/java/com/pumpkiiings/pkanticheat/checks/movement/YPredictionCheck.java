package com.pumpkiiings.pkanticheat.checks.movement;

import com.pumpkiiings.pkanticheat.checks.Check;
import com.pumpkiiings.pkanticheat.data.PlayerData;
import com.pumpkiiings.pkanticheat.data.PlayerDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class YPredictionCheck extends Check {

    public YPredictionCheck() {
        super("Y-Prediction");
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        if (event.player.level().isClientSide() || event.player.isCreative() || event.player.isSpectator() || event.player.isFallFlying()) return;
        if (event.player.isPassenger() || !(event.player instanceof ServerPlayer serverPlayer)) return;

        PlayerData data = PlayerDataManager.getData(serverPlayer);
        
        Vec3 curr = event.player.position();
        Vec3 last = data.getLastLocation();

        if (last == Vec3.ZERO) return;

        double deltaY = curr.y - last.y;
        double lastYDelta = data.getLastYDelta();
        
        boolean onGround = event.player.onGround();
        boolean wasOnGround = data.wasOnGround();

        // Bypass for water, lava, webs, climbing, taking knockback, or effects that change gravity
        if (event.player.isInWater() || event.player.isInLava() || event.player.onClimbable() || data.getVelocityTicks() > 0 || event.player.hasEffect(MobEffects.LEVITATION) || event.player.hasEffect(MobEffects.SLOW_FALLING)) {
            data.setLastYDelta(deltaY);
            // wasOnGround is updated by another class usually, but we sync it just in case
            data.setWasOnGround(onGround);
            return;
        }

        // We only check if they are in the air (both last tick and this tick) or just jumped.
        if (!onGround && !wasOnGround) {
            // Player is falling / in air
            // Expected gravity formula in Minecraft
            double expectedY = (lastYDelta - 0.08) * 0.98;
            
            // Allow 0.05 blocks of leniency for server-side tick desyncs
            double diff = Math.abs(deltaY - expectedY);

            if (diff > 0.05) {
                // Ignore small bounces on ground or block edges (like stepping down a slab)
                if (Math.abs(deltaY) > 0.01) {
                    data.flyVL++;
                    if (data.flyVL > 3) {
                        fail(serverPlayer, "Gravedad Invalida (Y-Prediction)");
                        // Setback
                        Vec3 safePos = data.getLastValidGroundLocation();
                        if (safePos != Vec3.ZERO) {
                            serverPlayer.connection.teleport(safePos.x, safePos.y, safePos.z, serverPlayer.getYRot(), serverPlayer.getXRot());
                        }
                    }
                }
            } else {
                data.flyVL = Math.max(0, data.flyVL - 1);
            }
        } else if (!onGround && wasOnGround && deltaY > 0) {
            // Player jumped
            double expectedJump = 0.42;
            if (event.player.hasEffect(MobEffects.JUMP)) {
                expectedJump += 0.1 * (event.player.getEffect(MobEffects.JUMP).getAmplifier() + 1);
            }
            
            double diff = Math.abs(deltaY - expectedJump);
            // Some blocks like slime or beds change jump physics, we allow leniency.
            // A normal step up a slab is 0.5, a full block is 1.0 (with horses).
            // We flag if they go higher than expected jump by a large margin.
            if (diff > 0.05 && deltaY > 0.6) {
                data.flyVL++;
                if (data.flyVL > 2) {
                    fail(serverPlayer, "Salto anormal (HighJump)");
                }
            }
        } else if (onGround) {
             data.flyVL = 0;
        }

        data.setLastYDelta(deltaY);
        // PlayerData handles wasOnGround in the global listener usually, but we ensure it's tracked if needed.
    }
}
