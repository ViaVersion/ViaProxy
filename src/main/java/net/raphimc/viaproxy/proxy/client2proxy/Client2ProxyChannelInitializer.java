package net.raphimc.viaproxy.proxy.client2proxy;

import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.connection.MinecraftChannelInitializer;
import net.raphimc.netminecraft.packet.registry.PacketRegistryUtil;
import net.raphimc.viaproxy.plugins.PluginManager;
import net.raphimc.viaproxy.plugins.events.Client2ProxyChannelInitializeEvent;
import net.raphimc.viaproxy.plugins.events.types.ITyped;

import java.util.function.Supplier;

public class Client2ProxyChannelInitializer extends MinecraftChannelInitializer {

    public Client2ProxyChannelInitializer(final Supplier<ChannelHandler> handlerSupplier) {
        super(handlerSupplier);
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) {
        if (PluginManager.EVENT_MANAGER.call(new Client2ProxyChannelInitializeEvent(ITyped.Type.PRE, socketChannel)).isCancelled()) {
            socketChannel.close();
            return;
        }

        super.initChannel(socketChannel);
        socketChannel.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(PacketRegistryUtil.getHandshakeRegistry(false));

        if (PluginManager.EVENT_MANAGER.call(new Client2ProxyChannelInitializeEvent(ITyped.Type.POST, socketChannel)).isCancelled()) {
            socketChannel.close();
        }
    }

}
