package com.pumpkiiings.pkanticheat.data;

import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private static final Map<UUID, PlayerData> dataMap = new ConcurrentHashMap<>();

    public static PlayerData getData(Player player) {
        return dataMap.computeIfAbsent(player.getUUID(), PlayerData::new);
    }

    public static void remove(Player player) {
        dataMap.remove(player.getUUID());
    }
    
    public static void clear() {
        dataMap.clear();
    }
}
