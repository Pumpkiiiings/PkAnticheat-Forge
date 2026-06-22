package com.pumpkiiings.pkanticheat;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AlertManager {
    private static final Logger LOGGER = LogManager.getLogger();
    // UUID -> (CheckName -> Stacks)
    private static final Map<UUID, Map<String, Integer>> violations = new ConcurrentHashMap<>();

    public static void addViolation(Player player, String checkName) {
        if (player.level().isClientSide()) return;

        UUID uuid = player.getUUID();
        violations.putIfAbsent(uuid, new ConcurrentHashMap<>());
        Map<String, Integer> playerViolations = violations.get(uuid);

        int stacks = playerViolations.getOrDefault(checkName, 0) + 1;
        playerViolations.put(checkName, stacks);

        int maxStacks = PkConfig.SERVER.maxStacks.get();

        if (stacks % maxStacks == 0) {
            String format = PkConfig.SERVER.alertFormat.get()
                    .replace("%player%", player.getName().getString())
                    .replace("%check%", checkName)
                    .replace("%stacks%", String.valueOf(stacks));
            
            // Reemplazar colores (soporta &)
            String coloredMessage = format.replace("&", "§");

            MinecraftServer server = player.getServer();
            if (server != null) {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    if (p.hasPermissions(2)) { // Nivel 2 = OP normalmente
                        p.sendSystemMessage(Component.literal(coloredMessage));
                    }
                }
                
                // Remove color codes for the console
                String plainMessage = coloredMessage.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
                LOGGER.info(plainMessage);
            }
        }
    }
    
    public static void clearViolations(Player player) {
        violations.remove(player.getUUID());
    }

    public static Map<String, Integer> getViolations(Player player) {
        return violations.getOrDefault(player.getUUID(), new HashMap<>());
    }
}
