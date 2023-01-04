package net.raphimc.viaproxy.proxy.proxy2server;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.connection.UserConnectionImpl;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;
import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.connection.MinecraftChannelInitializer;
import net.raphimc.netminecraft.packet.registry.PacketRegistryUtil;
import net.raphimc.vialegacy.netty.PreNettyDecoder;
import net.raphimc.vialegacy.netty.PreNettyEncoder;
import net.raphimc.vialegacy.protocols.release.protocol1_7_2_5to1_6_4.baseprotocols.PreNettyBaseProtocol;
import net.raphimc.vialegacy.util.VersionEnum;
import net.raphimc.viaprotocolhack.netty.ViaEncodeHandler;
import net.raphimc.viaprotocolhack.netty.ViaPipeline;
import net.raphimc.viaproxy.plugins.PluginManager;
import net.raphimc.viaproxy.plugins.events.Proxy2ServerChannelInitializeEvent;
import net.raphimc.viaproxy.plugins.events.types.ITyped;
import net.raphimc.viaproxy.protocolhack.impl.ViaProxyViaDecodeHandler;
import net.raphimc.viaproxy.proxy.ProxyConnection;

import java.util.function.Supplier;

public class Proxy2ServerChannelInitializer extends MinecraftChannelInitializer {

    public Proxy2ServerChannelInitializer(final Supplier<ChannelHandler> handlerSupplier) {
        super(handlerSupplier);
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) {
        if (PluginManager.EVENT_MANAGER.call(new Proxy2ServerChannelInitializeEvent(ITyped.Type.PRE, socketChannel)).isCancelled()) {
            socketChannel.close();
            return;
        }

        final UserConnection user = new UserConnectionImpl(socketChannel, true);
        new ProtocolPipelineImpl(user);
        ProxyConnection.fromChannel(socketChannel).setUserConnection(user);

        super.initChannel(socketChannel);
        socketChannel.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(PacketRegistryUtil.getHandshakeRegistry(true));
        socketChannel.pipeline().addBefore(MCPipeline.PACKET_CODEC_HANDLER_NAME, ViaPipeline.HANDLER_ENCODER_NAME, new ViaEncodeHandler(user));
        socketChannel.pipeline().addBefore(MCPipeline.PACKET_CODEC_HANDLER_NAME, ViaPipeline.HANDLER_DECODER_NAME, new ViaProxyViaDecodeHandler(user));

        if (ProxyConnection.fromChannel(socketChannel).getServerVersion().isOlderThanOrEqualTo(VersionEnum.r1_6_4)) {
            user.getProtocolInfo().getPipeline().add(PreNettyBaseProtocol.INSTANCE);
            socketChannel.pipeline().addBefore(MCPipeline.SIZER_HANDLER_NAME, ViaPipeline.HANDLER_PRE_NETTY_ENCODER_NAME, new PreNettyEncoder(user));
            socketChannel.pipeline().addBefore(MCPipeline.SIZER_HANDLER_NAME, ViaPipeline.HANDLER_PRE_NETTY_DECODER_NAME, new PreNettyDecoder(user));
        }

        if (PluginManager.EVENT_MANAGER.call(new Proxy2ServerChannelInitializeEvent(ITyped.Type.POST, socketChannel)).isCancelled()) {
            socketChannel.close();
        }
    }

}
