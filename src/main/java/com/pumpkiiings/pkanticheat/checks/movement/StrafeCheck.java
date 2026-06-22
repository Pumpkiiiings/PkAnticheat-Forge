package com.pumpkiiings.pkanticheat.checks.movement;

import com.pumpkiiings.pkanticheat.checks.Check;
import com.pumpkiiings.pkanticheat.data.PlayerData;
import com.pumpkiiings.pkanticheat.data.PlayerDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class StrafeCheck extends Check {

    public StrafeCheck() {
        super("Strafe");
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;

        if (event.player.level().isClientSide() || event.player.isCreative() || event.player.isFallFlying()) return;

        PlayerData data = PlayerDataManager.getData(event.player);
        Vec3 curr = event.player.position();
        Vec3 last = data.getLastLocation();

        if (!event.player.onGround() && !event.player.isInWater()) {
            data.airTicks++;
        } else {
            data.airTicks = 0;
            data.setBhopFlags(0);
            return;
        }

        if (last != Vec3.ZERO && data.airTicks > 3) {
            double deltaX = curr.x - last.x;
            double deltaZ = curr.z - last.z;
            double speedSq = deltaX * deltaX + deltaZ * deltaZ;

            // Speed potion effect (+20% base speed per level)
            int speedLevel = 0;
            if (event.player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                speedLevel = event.player.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() + 1;
            }
            double speedMultiplier = 1.0 + (0.2 * speedLevel);
            
            // In vanilla, sprint jump decays by ~9% per tick. 
            // After 3 ticks, speed should be well below 0.35 (without potions).
            // Strafe/Bhop hacks forcefully maintain it higher.
            double maxExpectedAirSpeed = 0.34 * speedMultiplier;
            double maxSpeedSq = maxExpectedAirSpeed * maxExpectedAirSpeed;

            if (speedSq > maxSpeedSq) { 
                data.setBhopFlags(data.getBhopFlags() + 1);
                
                if (data.getBhopFlags() >= 5) { // Needs 5 consecutive abnormal ticks
                    fail(event.player, "Abnormal air acceleration (Strafe/Bhop)");
                    if (event.player instanceof ServerPlayer sp) {
                        Vec3 safe = data.getLastValidGroundLocation();
                        if (safe != Vec3.ZERO) {
                            sp.connection.teleport(safe.x, safe.y, safe.z, sp.getYRot(), sp.getXRot());
                        }
                    }
                    data.setBhopFlags(0);
                }
            } else {
                if (data.getBhopFlags() > 0) {
                    data.setBhopFlags(data.getBhopFlags() - 1); // reduce flag organically
                }
            }
        }
    }
}
