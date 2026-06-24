package com.pumpkiiings.pkanticheat.events;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class PlayerPacketEvent extends Event {
    private final ServerPlayer player;
    private final Packet<?> packet;
    private final PacketDirection direction;

    public PlayerPacketEvent(ServerPlayer player, Packet<?> packet, PacketDirection direction) {
        this.player = player;
        this.packet = packet;
        this.direction = direction;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public PacketDirection getDirection() {
        return direction;
    }

    public enum PacketDirection {
        INBOUND,
        OUTBOUND
    }
}
