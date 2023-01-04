package net.raphimc.viaproxy.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.raphimc.netminecraft.packet.PacketTypes;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginKeyPacket1_19;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.util.LocalSocketClient;
import net.raphimc.viaproxy.util.logging.Logger;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.*;

public class CustomPayloadInterface {

    public static final String OPENAUTHMOD_BASE_CHANNEL = "oam:";
    public static final byte[] OPENAUTHMOD_LEGACY_MAGIC_BYTES = new byte[]{2, 20, 12, 3}; // 1.8 - 1.12.2
    public static final String OPENAUTHMOD_LEGACY_MAGIC_STRING = new String(OPENAUTHMOD_LEGACY_MAGIC_BYTES, StandardCharsets.UTF_8); // 1.8 - 1.12.2
    public static final int OPENAUTHMOD_LEGACY_MAGIC_INT = new BigInteger(OPENAUTHMOD_LEGACY_MAGIC_BYTES).intValueExact(); // 1.8 - 1.12.2

    // Request
    public static final String OPENAUTHMOD_JOIN_CHANNEL = OPENAUTHMOD_BASE_CHANNEL + "join"; // 1.8 - latest
    public static final String OPENAUTHMOD_SIGN_NONCE_CHANNEL = OPENAUTHMOD_BASE_CHANNEL + "sign_nonce"; // 1.19 - latest

    // Response
    public static final String OPENAUTHMOD_DATA_CHANNEL = OPENAUTHMOD_BASE_CHANNEL + "data"; // 1.8 - latest

    public static void joinServer(final String serverIdHash, final ProxyConnection proxyConnection) throws InterruptedException, ExecutionException {
        Logger.u_info("auth", proxyConnection.getC2P().remoteAddress(), proxyConnection.getGameProfile(), "Trying to join online mode server");
        if (Options.OPENAUTHMOD_AUTH) {
            try {
                final ByteBuf response = proxyConnection.sendCustomPayload(OPENAUTHMOD_JOIN_CHANNEL, PacketTypes.writeString(Unpooled.buffer(), serverIdHash)).get(6, TimeUnit.SECONDS);
                if (response == null) throw new TimeoutException();
                if (response.isReadable() && !response.readBoolean()) throw new TimeoutException();
            } catch (TimeoutException e) {
                proxyConnection.kickClient("§cAuthentication cancelled! You need to install OpenAuthMod in order to join this server.");
            }
        } else if (Options.LOCAL_SOCKET_AUTH) {
            new LocalSocketClient(48941).request("authenticate", serverIdHash);
        }
    }

    public static void signNonce(final byte[] nonce, final C2SLoginKeyPacket1_19 packet, final ProxyConnection proxyConnection) throws InterruptedException, ExecutionException {
        Logger.u_info("auth", proxyConnection.getC2P().remoteAddress(), proxyConnection.getGameProfile(), "Requesting nonce signature");
        if (Options.OPENAUTHMOD_AUTH) {
            try {
                final ByteBuf response = proxyConnection.sendCustomPayload(OPENAUTHMOD_SIGN_NONCE_CHANNEL, PacketTypes.writeByteArray(Unpooled.buffer(), nonce)).get(4, TimeUnit.SECONDS);
                if (response == null) throw new TimeoutException();
                if (!response.readBoolean()) throw new TimeoutException();
                packet.salt = response.readLong();
                packet.signature = PacketTypes.readByteArray(response);
            } catch (TimeoutException e) {
                proxyConnection.kickClient("§cAuthentication cancelled! You need to install OpenAuthMod in order to join this server.");
            }
        } else if (Options.LOCAL_SOCKET_AUTH) {
            final String[] response = new LocalSocketClient(48941).request("sign_nonce", Base64.getEncoder().encodeToString(nonce));
            if (response != null && response[0].equals("success")) {
                packet.salt = Long.valueOf(response[1]);
                packet.signature = Base64.getDecoder().decode(response[2]);
            }
        }
    }

}
