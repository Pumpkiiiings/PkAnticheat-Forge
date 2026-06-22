package com.pumpkiiings.pkanticheat;

import com.pumpkiiings.pkanticheat.checks.combat.KillauraCheck;
import com.pumpkiiings.pkanticheat.checks.combat.ReachCheck;
import com.pumpkiiings.pkanticheat.checks.movement.FlyCheck;
import com.pumpkiiings.pkanticheat.checks.movement.NoSlowCheck;
import com.pumpkiiings.pkanticheat.checks.packet.DamageIndicatorSpoofCheck;
import com.pumpkiiings.pkanticheat.checks.world.FastBreakCheck;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BootManager {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void boot() {
        LOGGER.info("[PkAnticheat] Booting Stage 1: Verifying Configuration...");
        if (PkConfig.SERVER.maxStacks.get() <= 0) {
            LOGGER.warn("[PkAnticheat] Invalid configuration detected!");
        }

        LOGGER.info("[PkAnticheat] Booting Stage 2: Loading Combat Modules...");
        MinecraftForge.EVENT_BUS.register(new ReachCheck());
        MinecraftForge.EVENT_BUS.register(new KillauraCheck());
        MinecraftForge.EVENT_BUS.register(new com.pumpkiiings.pkanticheat.checks.combat.VelocityCheck());
        MinecraftForge.EVENT_BUS.register(new com.pumpkiiings.pkanticheat.checks.combat.TriggerBotCheck());

        LOGGER.info("[PkAnticheat] Booting Stage 3: Loading Movement Modules...");
        MinecraftForge.EVENT_BUS.register(new FlyCheck());
        MinecraftForge.EVENT_BUS.register(new NoSlowCheck());
        MinecraftForge.EVENT_BUS.register(new com.pumpkiiings.pkanticheat.checks.movement.StrafeCheck());
        MinecraftForge.EVENT_BUS.register(new com.pumpkiiings.pkanticheat.checks.movement.SpiderCheck());
        MinecraftForge.EVENT_BUS.register(new com.pumpkiiings.pkanticheat.checks.movement.WaterWalkCheck());
        MinecraftForge.EVENT_BUS.register(new com.pumpkiiings.pkanticheat.checks.movement.AntiVoidCheck());
        MinecraftForge.EVENT_BUS.register(new com.pumpkiiings.pkanticheat.checks.movement.NoFallCheck());
        MinecraftForge.EVENT_BUS.register(new com.pumpkiiings.pkanticheat.checks.movement.SafeWalkCheck());
        MinecraftForge.EVENT_BUS.register(new com.pumpkiiings.pkanticheat.checks.movement.SimplePredictionCheck());

        LOGGER.info("[PkAnticheat] Booting Stage 4: Loading World Modules...");
        MinecraftForge.EVENT_BUS.register(new FastBreakCheck());

        LOGGER.info("[PkAnticheat] Booting Stage 5: Loading Packet Protections...");
        MinecraftForge.EVENT_BUS.register(new DamageIndicatorSpoofCheck());

        LOGGER.info("[PkAnticheat] Boot completed successfully! All checks are active.");
    }
}
