package com.pumpkiiings.pkanticheat;

import com.pumpkiiings.pkanticheat.commands.PkCommand;
import com.pumpkiiings.pkanticheat.data.PlayerDataManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.event.server.ServerStartingEvent;

@Mod("pkanticheat")
public class PkAnticheat {

    public PkAnticheat() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, PkConfig.SERVER_SPEC);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        BootManager.boot();
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        AlertManager.clearViolations(event.getEntity());
        PlayerDataManager.remove(event.getEntity());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        PkCommand.register(event.getDispatcher());
    }
}
