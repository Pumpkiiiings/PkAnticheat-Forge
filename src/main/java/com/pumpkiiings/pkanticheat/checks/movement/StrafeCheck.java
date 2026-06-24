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

        if (!event.player.onGround() && !event.player.isInWater() && !event.player.isInLava() && !event.player.onClimbable()) {
            data.airTicks++;
        } else {
            data.airTicks = 0;
            data.setBhopFlags(0);
            return;
        }

        // Evitar falsos positivos por empuje o daño
        if (event.player.hurtTime > 0) {
            data.setBhopFlags(0);
            return;
        }

        if (last != Vec3.ZERO && data.airTicks > 2) {
            double deltaX = curr.x - last.x;
            double deltaZ = curr.z - last.z;
            double actualSpeedSq = deltaX * deltaX + deltaZ * deltaZ;

            if (actualSpeedSq < 0.005) {
                // Muy lento para ser evaluado
                data.setBhopFlags(Math.max(0, data.getBhopFlags() - 1));
                return;
            }

            // --- Heurística 1: Límites Físicos (Speed Check) ---
            // Speed potion effect (+20% base speed per level)
            int speedLevel = 0;
            if (event.player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                speedLevel = event.player.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() + 1;
            }
            double speedMultiplier = 1.0 + (0.2 * speedLevel);
            
            // En vanilla, el salto con sprint da una ráfaga inicial de ~0.35 a ~0.36, pero CADA TICK en el aire
            // se multiplica por 0.91 (fricción del aire). 
            // Strafe y Bhop mantienen la velocidad en el máximo artificialmente.
            // Tolerancia de latencia (0.02)
            double maxExpectedAirSpeed = 0.36 * speedMultiplier;
            double maxSpeedSq = maxExpectedAirSpeed * maxExpectedAirSpeed;

            boolean failedSpeed = false;
            // Si el jugador acelera extrañamente en el aire o mantiene max speed
            if (actualSpeedSq > maxSpeedSq + 0.01) { 
                failedSpeed = true;
            }

            // --- Heurística 2: Direccionalidad (Angle Check) ---
            // Un jugador real solo puede moverse en 8 direcciones relativas a su Yaw (W, A, S, D y diagonales).
            // Un cliente con Strafe "redondea" la velocidad para deslizarse suavemente, creando ángulos no-vanilla.
            float playerYaw = event.player.getYRot();
            double motionAngle = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0; // Ajuste para el sistema de coordenadas de MC
            
            // Normalizar ángulos a 360 grados
            playerYaw = (playerYaw % 360 + 360) % 360;
            motionAngle = (motionAngle % 360 + 360) % 360;

            // Calcular diferencia
            double angleDiff = Math.abs(playerYaw - motionAngle);
            if (angleDiff > 180) angleDiff = 360 - angleDiff;

            // En vanilla, el ángulo de movimiento debería estar cerca de 0, 45, 90, 135, o 180 grados de diferencia respecto al Yaw.
            double minOffsetToValidInput = Math.min(angleDiff % 45, 45 - (angleDiff % 45));

            boolean failedDirection = false;
            // Si el ángulo de movimiento está desfasado por más de 12 grados de los inputs válidos, es Strafe.
            if (minOffsetToValidInput > 15.0 && actualSpeedSq > 0.05) {
                failedDirection = true;
            }

            if (failedSpeed || failedDirection) {
                data.setBhopFlags(data.getBhopFlags() + 1);
                
                if (data.getBhopFlags() >= 5) { // Needs 5 consecutive abnormal ticks
                    String flagReason = failedSpeed ? "Air Speed" : "Move Angle";
                    fail(event.player, "Abnormal air acceleration/movement (Strafe/Bhop) [" + flagReason + "]");
                    setback(event.player);
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
