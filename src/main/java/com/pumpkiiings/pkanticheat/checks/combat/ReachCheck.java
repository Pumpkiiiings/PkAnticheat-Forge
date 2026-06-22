package com.pumpkiiings.pkanticheat.checks.combat;

import com.pumpkiiings.pkanticheat.checks.Check;

import com.pumpkiiings.pkanticheat.AlertManager;
import com.pumpkiiings.pkanticheat.PkConfig;
import com.pumpkiiings.pkanticheat.data.PlayerData;
import com.pumpkiiings.pkanticheat.data.PlayerDataManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ReachCheck extends Check {

    public ReachCheck() {
        super("Reach");
    }

    @SubscribeEvent
    public void onPlayerAttack(AttackEntityEvent event) {
        Player attacker = event.getEntity();
        Entity target = event.getTarget();

        if (attacker.level().isClientSide() || attacker.isCreative() || !(attacker instanceof ServerPlayer serverAttacker)) {
            return;
        }

        PlayerData data = PlayerDataManager.getData(serverAttacker);
        data.updatePing(serverAttacker.latency);

        Vec3 eyePos = attacker.getEyePosition();
        Vec3 targetPos = target.position();
        
        double distance = eyePos.distanceTo(targetPos);
        
        // Base max reach in vanilla is 3.0 blocks
        // We add a small buffer for bounding box edges
        double maxLegalReach = 3.5;
        
        // Dynamic Ping Compensation
        // Add 0.5 blocks per 100ms of ping to compensate for Hitbox Desync
        double pingBuffer = (data.currentPing / 100.0) * 0.5;
        if (data.pingJitter > 100) {
            pingBuffer += 1.0; // Huge buffer for lag spikes
        }
        
        maxLegalReach += pingBuffer;

        if (distance > maxLegalReach) {
            fail(attacker, "Reach (" + String.format("%.2f", distance) + " > " + String.format("%.2f", maxLegalReach) + ")");
            event.setCanceled(true);
            return;
        }

        // 2. WallHit Check (Raytrace)
        Vec3 targetCenter = target.position().add(0, target.getBbHeight() / 2, 0);

        HitResult result = attacker.level().clip(new ClipContext(
                eyePos,
                targetCenter,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                attacker
        ));

        if (result.getType() == HitResult.Type.BLOCK) {
            double distToBlock = eyePos.distanceTo(result.getLocation());
            double distToEntity = eyePos.distanceTo(targetCenter);
            if (distToBlock < distToEntity - 0.5) { // 0.5 margin for hitboxes near walls
                AlertManager.addViolation(attacker, "WallHit");
                event.setCanceled(true);
            }
        }
    }
}
