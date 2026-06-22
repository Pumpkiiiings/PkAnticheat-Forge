package com.pumpkiiings.pkanticheat.checks.packet;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PacketInjector extends ChannelDuplexHandler {
    private final ServerPlayer player;
    private static final Random RANDOM = new Random();

    public PacketInjector(ServerPlayer player) {
        this.player = player;
    }

    public static void inject(ServerPlayer player) {
        Connection connection = player.connection.connection;
        Channel channel = connection.channel();

        if (channel.pipeline().get("pkanticheat_injector") == null) {
            channel.pipeline().addBefore("packet_handler", "pkanticheat_injector", new PacketInjector(player));
        }
    }

    public static void remove(ServerPlayer player) {
        Connection connection = player.connection.connection;
        Channel channel = connection.channel();

        if (channel.pipeline().get("pkanticheat_injector") != null) {
            channel.pipeline().remove("pkanticheat_injector");
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof net.minecraft.network.protocol.game.ServerboundMovePlayerPacket packet) {
            TimerCheck.handle(player);
            BlinkCheck.handle(player);
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ClientboundSetEntityDataPacket packet) {
            // Do NOT spoof the player's own entity data, otherwise their health bar will glitch!
            if (packet.id() != player.getId()) {
                List<SynchedEntityData.DataValue<?>> packedItems = packet.packedItems();
                if (packedItems != null) {
                    boolean modified = false;
                    List<SynchedEntityData.DataValue<?>> newItems = new ArrayList<>();
    
                    for (SynchedEntityData.DataValue<?> item : packedItems) {
                        if (item.value() instanceof Float fValue) {
                            // Spoof float data (which is usually Health) to random values 5.0, 10.0, 15.0, 20.0
                            float fakeHealth = (RANDOM.nextInt(4) + 1) * 5.0f; 
                            
                            @SuppressWarnings("unchecked")
                            SynchedEntityData.DataValue<Float> floatItem = (SynchedEntityData.DataValue<Float>) item;
                            newItems.add(new SynchedEntityData.DataValue<>(floatItem.id(), floatItem.serializer(), fakeHealth));
                            modified = true;
                        } else {
                            newItems.add(item);
                        }
                    }
    
                    if (modified) {
                        msg = new ClientboundSetEntityDataPacket(packet.id(), newItems);
                    }
                }
            }
        }
        super.write(ctx, msg, promise);
    }
}
