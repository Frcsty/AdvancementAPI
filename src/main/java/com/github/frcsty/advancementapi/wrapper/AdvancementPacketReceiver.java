package com.github.frcsty.advancementapi.wrapper;

import com.github.frcsty.advancementapi.AdvancementAPI;
import com.github.frcsty.advancementapi.event.AdvancementScreenCloseEvent;
import com.github.frcsty.advancementapi.event.AdvancementTabChangeEvent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageToMessageDecoder;
import net.minecraft.server.v1_16_R1.NetworkManager;
import net.minecraft.server.v1_16_R1.Packet;
import net.minecraft.server.v1_16_R1.PacketPlayInAdvancements;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AdvancementPacketReceiver {

    private static Map<String, ChannelHandler> handlers = new HashMap<>();
    private static Field channelField;

    static {
        for (final Field f : NetworkManager.class.getDeclaredFields()) {
            if (f.getType().isAssignableFrom(Channel.class)) {
                channelField = f;
                channelField.setAccessible(true);
                break;
            }
        }
    }

    public ChannelHandler listen(final Player p, final PacketReceivingHandler handler) {
        final Channel ch = getNettyChannel(p);
        final ChannelPipeline pipe = ch.pipeline();

        final ChannelHandler handle = new MessageToMessageDecoder<Packet>() {
            @Override
            protected void decode(ChannelHandlerContext chc, Packet packet, List<Object> out) throws Exception {

                if (packet instanceof PacketPlayInAdvancements) {
                    if (!handler.handle(p, (PacketPlayInAdvancements) packet)) {
                        out.add(packet);
                    }
                    return;
                }

                out.add(packet);
            }
        };
        pipe.addAfter("decoder", "advancements_listener_" + handler.hashCode(), handle);

        return handle;
    }

    public Channel getNettyChannel(final Player p) {
        final NetworkManager manager = ((CraftPlayer) p).getHandle().playerConnection.networkManager;
        Channel channel = null;
        try {
            channel = (Channel) channelField.get(manager);
        } catch (final IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return channel;
    }

    public void close(final Player p, final ChannelHandler handler) {
        try {
            getNettyChannel(p).pipeline().remove(handler);
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    public Map<String, ChannelHandler> getHandlers() {
        return handlers;
    }

    public void initPlayer(final Player p) {
        handlers.put(p.getName(), listen(p, new PacketReceivingHandler() {

            @Override
            public boolean handle(final Player p, final PacketPlayInAdvancements packet) {

                if (packet.c() == PacketPlayInAdvancements.Status.OPENED_TAB) {
                    final NameKey name = new NameKey(packet.d());
                    final AdvancementTabChangeEvent event = new AdvancementTabChangeEvent(p, name);
                    Bukkit.getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        AdvancementAPI.clearActiveTab(p);
                        return false;
                    } else {
                        if (!event.getTabAdvancement().equals(name)) {
                            AdvancementAPI.setActiveTab(p, event.getTabAdvancement());
                        } else {
                            AdvancementAPI.setActiveTab(p, name, false);
                        }
                    }
                } else {
                    final AdvancementScreenCloseEvent event = new AdvancementScreenCloseEvent(p);
                    Bukkit.getPluginManager().callEvent(event);
                }

                return true;
            }
        }));
    }

    interface PacketReceivingHandler {
        boolean handle(final Player p, final PacketPlayInAdvancements packet);
    }

}
