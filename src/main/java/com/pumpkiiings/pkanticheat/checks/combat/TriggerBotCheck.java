package com.pumpkiiings.pkanticheat.checks.combat;

import com.pumpkiiings.pkanticheat.checks.Check;
import com.pumpkiiings.pkanticheat.data.PlayerData;
import com.pumpkiiings.pkanticheat.data.PlayerDataManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class TriggerBotCheck extends Check {

    public TriggerBotCheck() {
        super("TriggerBot");
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.isCreative()) return;

        PlayerData data = PlayerDataManager.getData(player);
        Entity target = event.getTarget();

        long now = System.currentTimeMillis();

        if (data.getLastLookedEntity() != target) {
            data.setLastLookedEntity(target);
            data.setLastLookedEntityTime(now);
            
            float yawDelta = Math.abs(player.getYRot() - data.getLastYaw());
            float pitchDelta = Math.abs(player.getXRot() - data.getLastPitch());
            
            if (yawDelta > 15.0f && pitchDelta > 5.0f) {
                // Large snap directly to an attack is suspicious
                fail(player, "Instant attack after large snap (TriggerBot/Aimbot)");
                event.setCanceled(true);
            }
        } else {
            long timeSinceFirstLook = now - data.getLastLookedEntityTime();
            if (timeSinceFirstLook < 5) { // Attacked < 5ms after targeting
                fail(player, "Inhuman reaction time (TriggerBot)");
                event.setCanceled(true);
            }
        }
    }
}
