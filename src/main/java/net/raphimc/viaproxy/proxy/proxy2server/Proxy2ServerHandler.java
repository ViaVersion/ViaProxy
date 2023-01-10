package net.raphimc.viaproxy.proxy.proxy2server;

import com.github.steveice10.mc.auth.data.GameProfile;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.crypto.AESEncryption;
import net.raphimc.netminecraft.netty.crypto.CryptUtil;
import net.raphimc.netminecraft.packet.IPacket;
import net.raphimc.netminecraft.packet.impl.login.*;
import net.raphimc.vialegacy.protocols.release.protocol1_7_2_5to1_6_4.storage.ProtocolMetadataStorage;
import net.raphimc.viaprotocolhack.util.VersionEnum;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.proxy.ExternalInterface;
import net.raphimc.viaproxy.proxy.ProxyConnection;
import net.raphimc.viaproxy.proxy.util.ExceptionUtil;
import net.raphimc.viaproxy.util.logging.Logger;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.concurrent.ExecutionException;

public class Proxy2ServerHandler extends SimpleChannelInboundHandler<IPacket> {

    private ProxyConnection proxyConnection;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        this.proxyConnection = ProxyConnection.fromChannel(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);

        Logger.u_info("disconnect", this.proxyConnection.getC2P().remoteAddress(), this.proxyConnection.getGameProfile(), "Connection closed");
        try {
            this.proxyConnection.getC2P().close();
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IPacket packet) throws Exception {
        if (this.proxyConnection.isClosed()) return;

        switch (this.proxyConnection.getConnectionState()) {
            case LOGIN:
                if (packet instanceof S2CLoginKeyPacket1_7) this.handleLoginKey((S2CLoginKeyPacket1_7) packet);
                else if (packet instanceof S2CLoginSuccessPacket1_7) this.handleLoginSuccess((S2CLoginSuccessPacket1_7) packet);
                else if (packet instanceof S2CLoginCompressionPacket) this.handleLoginCompression((S2CLoginCompressionPacket) packet);
                else break;

                return;
        }

        this.proxyConnection.getC2P().writeAndFlush(packet).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ExceptionUtil.handleNettyException(ctx, cause, this.proxyConnection);
    }

    private void handleLoginKey(final S2CLoginKeyPacket1_7 packet) throws InterruptedException, GeneralSecurityException, ExecutionException {
        final PublicKey publicKey = CryptUtil.decodeRsaPublicKey(packet.publicKey);
        final SecretKey secretKey = CryptUtil.generateSecretKey();
        final String serverHash = new BigInteger(CryptUtil.computeServerIdHash(packet.serverId, publicKey, secretKey)).toString(16);

        boolean auth = true;
        if (this.proxyConnection.getServerVersion().isOlderThanOrEqualTo(VersionEnum.r1_6_4)) {
            auth = this.proxyConnection.getUserConnection().get(ProtocolMetadataStorage.class).authenticate;
        }
        if (auth) {
            ExternalInterface.joinServer(serverHash, this.proxyConnection);
        }

        final byte[] encryptedSecretKey = CryptUtil.encryptData(publicKey, secretKey.getEncoded());
        final byte[] encryptedNonce = CryptUtil.encryptData(publicKey, packet.nonce);

        final C2SLoginKeyPacket1_19_3 loginKey = new C2SLoginKeyPacket1_19_3(encryptedSecretKey, encryptedNonce);
        if (this.proxyConnection.getServerVersion().isNewerThanOrEqualTo(VersionEnum.r1_19) && this.proxyConnection.getLoginHelloPacket() instanceof C2SLoginHelloPacket1_19 && ((C2SLoginHelloPacket1_19) this.proxyConnection.getLoginHelloPacket()).key != null) {
            ExternalInterface.signNonce(packet.nonce, loginKey, this.proxyConnection);
        }
        this.proxyConnection.getChannel().writeAndFlush(loginKey).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

        if (this.proxyConnection.getServerVersion().isNewerThanOrEqualTo(VersionEnum.r1_7_2tor1_7_5)) {
            this.proxyConnection.getChannel().attr(MCPipeline.ENCRYPTION_ATTRIBUTE_KEY).set(new AESEncryption(secretKey));
        } else {
            this.proxyConnection.setKeyForPreNettyEncryption(secretKey);
        }
    }

    private void handleLoginSuccess(final S2CLoginSuccessPacket1_7 packet) throws Exception {
        if (this.proxyConnection.getClientVersion().isNewerThanOrEqualTo(VersionEnum.r1_8)) {
            if (Options.COMPRESSION_THRESHOLD > -1) {
                this.proxyConnection.getC2P().writeAndFlush(new S2CLoginCompressionPacket(Options.COMPRESSION_THRESHOLD)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE).await();
                this.proxyConnection.getC2P().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).set(Options.COMPRESSION_THRESHOLD);
            }
        }

        this.proxyConnection.setGameProfile(new GameProfile(packet.uuid, packet.name));

        Logger.u_info("connect", this.proxyConnection.getC2P().remoteAddress(), this.proxyConnection.getGameProfile(), "Connected successfully! Switching to PLAY state");
        this.proxyConnection.getC2P().writeAndFlush(packet).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE).await();
        this.proxyConnection.setConnectionState(ConnectionState.PLAY);
    }

    private void handleLoginCompression(final S2CLoginCompressionPacket packet) {
        this.proxyConnection.getChannel().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).set(packet.compressionThreshold);
    }

}
