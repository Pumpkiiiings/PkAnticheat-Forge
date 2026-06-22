package com.pumpkiiings.pkanticheat.checks.combat;

import com.pumpkiiings.pkanticheat.checks.Check;

import com.pumpkiiings.pkanticheat.PkConfig;
import com.pumpkiiings.pkanticheat.data.PlayerData;
import com.pumpkiiings.pkanticheat.data.PlayerDataManager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class KillauraCheck extends Check {

    public KillauraCheck() {
        super("Killaura");
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.isCreative()) return;

        PlayerData data = PlayerDataManager.getData(player);

        float yaw = player.getYRot();
        float pitch = player.getXRot();

        float yawDelta = Math.abs(yaw - data.getLastYaw());
        float pitchDelta = Math.abs(pitch - data.getLastPitch());

        data.setLastYaw(yaw);
        data.setLastPitch(pitch);

        // Store deltas
        data.yawDeltas.add(yawDelta);
        data.pitchDeltas.add(pitchDelta);

        if (data.yawDeltas.size() > 10) {
            data.yawDeltas.poll();
            data.pitchDeltas.poll();

            // Calculate standard deviation of yaw deltas
            double sum = 0;
            for (float d : data.yawDeltas) sum += d;
            double mean = sum / 10.0;

            double standardDeviationSum = 0;
            for (float d : data.yawDeltas) {
                standardDeviationSum += Math.pow(d - mean, 2);
            }
            double deviation = Math.sqrt(standardDeviationSum / 10.0);

            // Low deviation implies aimbot
            if (deviation > 0 && deviation < PkConfig.SERVER.killauraDeviationThreshold.get() && yawDelta > 0) {
                fail(player, "Abnormal rotation deviation: " + String.format("%.4f", deviation));
                event.setCanceled(true);
                // Clear to prevent spam
                data.yawDeltas.clear();
                data.pitchDeltas.clear();
            }
        }
    }
}
