package com.pumpkiiings.pkanticheat.events;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

public class PacketManager {

    private static final String HANDLER_NAME = "pkanticheat_packet_handler";
    private static final Logger LOGGER = LogManager.getLogger();

    // Resolve the private 'channel' field on Connection via reflection at class load time
    private static final Field CHANNEL_FIELD;

    static {
        Field f = null;
        try {
            // In 1.20.1 official mappings the field is named "channel"
            f = Connection.class.getDeclaredField("channel");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            LOGGER.error("[PkAnticheat] Could not find 'channel' field in Connection. Netty injection will be disabled.", e);
        }
        CHANNEL_FIELD = f;
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            injectPlayer(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            removePlayer(player);
        }
    }

    private Channel getChannel(ServerPlayer player) {
        if (CHANNEL_FIELD == null) return null;
        try {
            return (Channel) CHANNEL_FIELD.get(player.connection.connection);
        } catch (IllegalAccessException e) {
            LOGGER.error("[PkAnticheat] Failed to access Netty channel for player " + player.getName().getString(), e);
            return null;
        }
    }

    private void injectPlayer(ServerPlayer player) {
        Channel channel = getChannel(player);
        if (channel == null) return;

        try {
            // Remove stale handler if present (e.g. player reconnected)
            if (channel.pipeline().get(HANDLER_NAME) != null) {
                channel.pipeline().remove(HANDLER_NAME);
            }

            channel.pipeline().addBefore("packet_handler", HANDLER_NAME, new ChannelDuplexHandler() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    if (msg instanceof Packet<?> packet) {
                        PlayerPacketEvent event = new PlayerPacketEvent(
                                player, packet, PlayerPacketEvent.PacketDirection.INBOUND);
                        if (MinecraftForge.EVENT_BUS.post(event)) {
                            // Canceled – drop packet silently
                            return;
                        }
                    }
                    super.channelRead(ctx, msg);
                }

                @Override
                public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                    if (msg instanceof Packet<?> packet) {
                        PlayerPacketEvent event = new PlayerPacketEvent(
                                player, packet, PlayerPacketEvent.PacketDirection.OUTBOUND);
                        if (MinecraftForge.EVENT_BUS.post(event)) {
                            // Canceled – drop packet silently
                            promise.setSuccess();
                            return;
                        }
                    }
                    super.write(ctx, msg, promise);
                }
            });

            LOGGER.debug("[PkAnticheat] Injected Netty handler for {}", player.getName().getString());
        } catch (Exception e) {
            LOGGER.error("[PkAnticheat] Failed to inject Netty handler for " + player.getName().getString(), e);
        }
    }

    private void removePlayer(ServerPlayer player) {
        Channel channel = getChannel(player);
        if (channel == null) return;
        try {
            if (channel.pipeline().get(HANDLER_NAME) != null) {
                channel.pipeline().remove(HANDLER_NAME);
            }
        } catch (Exception ignored) {
            // Channel may already be closed on disconnect
        }
    }
}
