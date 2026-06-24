package com.pumpkiiings.pkanticheat.checks.combat;

import com.pumpkiiings.pkanticheat.checks.Check;
import com.pumpkiiings.pkanticheat.data.PlayerData;
import com.pumpkiiings.pkanticheat.data.PlayerDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Optional;

public class RaytraceCheck extends Check {

    public RaytraceCheck() {
        super("Hitbox");
    }

    @SubscribeEvent
    public void onPlayerAttack(AttackEntityEvent event) {
        Player attacker = event.getEntity();
        Entity target = event.getTarget();

        if (attacker.level().isClientSide() || attacker.isCreative() || !(attacker instanceof ServerPlayer serverAttacker)) {
            return;
        }

        PlayerData data = PlayerDataManager.getData(serverAttacker);

        // Calculate Eye Position and View Vector
        Vec3 eyePos = attacker.getEyePosition();
        Vec3 lookVec = attacker.getViewVector(1.0F); // Vector 3D exacto usando Pitch y Yaw

        // Calculate max reach vector
        double maxLegalReach = 3.2; // Vanilla limit is 3.0, we add 0.2 base tolerance
        Vec3 reachEnd = eyePos.add(lookVec.scale(maxLegalReach));

        // Get target's bounding box
        AABB targetBox = target.getBoundingBox();

        // Expand Bounding Box to compensate for latency (Hitbox desync)
        double pingBuffer = (data.currentPing / 100.0) * 0.15; // +0.15 blocks expansion per 100ms ping
        if (data.pingJitter > 50) {
            pingBuffer += 0.2; // Add extra buffer for jitter
        }
        
        // Expansion is always minimum 0.1 for float rounding errors
        double expansion = Math.max(0.1, pingBuffer);
        
        AABB expandedBox = targetBox.inflate(expansion);

        // Raytrace intersection: Check if the line from Eye to ReachEnd hits the Box
        Optional<Vec3> hitResult = expandedBox.clip(eyePos, reachEnd);

        if (hitResult.isEmpty()) {
            // The ray did not intersect the target's bounding box at all!
            // This means they attacked the air, but the packet claims they hit the entity.
            fail(attacker, "Atacó fuera de la hitbox (Hitbox/Reach). Expansión: " + String.format("%.2f", expansion));
            event.setCanceled(true);
        } else {
            // Check Reach Distance exactly from the hit point
            Vec3 hitPoint = hitResult.get();
            double distanceToHit = eyePos.distanceTo(hitPoint);
            
            // Vanilla strictly limits the distance to the *intersection* point to <= 3.0 blocks
            double strictMaxDistance = 3.0 + (pingBuffer * 0.5); 
            
            if (distanceToHit > strictMaxDistance) {
                fail(attacker, "Alcance máximo superado (Reach). Distancia: " + String.format("%.2f", distanceToHit));
                event.setCanceled(true);
            }
        }
    }
}
