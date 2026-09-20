/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2026 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.viaproxy.proxy.session;

import com.google.gson.JsonObject;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.libs.fastutil.objects.ObjectArrayList;
import io.jsonwebtoken.Jwts;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramChannel;
import io.netty.util.concurrent.ScheduledFuture;
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager;
import net.raphimc.minecraftauth.bedrock.model.MinecraftMultiplayerToken;
import net.raphimc.netminecraft.util.EventLoops;
import net.raphimc.netminecraft.util.TransportType;
import net.raphimc.viabedrock.netty.raknet.MessageCodec;
import net.raphimc.viabedrock.protocol.data.ProtocolConstants;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.saves.impl.accounts.BedrockAccount;
import net.raphimc.viaproxy.util.address.*;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChannelFactory;
import org.cloudburstmc.netty.channel.nethernet.NetherNetConstants;
import org.cloudburstmc.netty.channel.nethernet.config.NetherChannelOption;
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetClientSignaling;
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetDiscoverySignaling;
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetXboxRpcSignaling;
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetXboxSignaling;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.cloudburstmc.netty.channel.raknet.RakClientChannel;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;
import org.cloudburstmc.netty.util.nethernet.IdentityUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class BedrockProxyConnection extends ProxyConnection {

    private SocketAddress serverAddress;

    public BedrockProxyConnection(final ChannelInitializer<Channel> channelInitializer, final Channel c2p) {
        super(channelInitializer, c2p);
    }

    @Override
    public void initialize(final TransportType transportType, final Bootstrap bootstrap) {
        bootstrap
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ViaProxy.getConfig().getConnectTimeout())
                .attr(ProxyConnection.PROXY_CONNECTION_ATTRIBUTE_KEY, this)
                .handler(this.channelInitializer);

        if (this.serverAddress instanceof NetherNetAddress) {
            this.initializeNetherNet(transportType, bootstrap);
        } else {
            this.initializeRakNet(transportType, bootstrap);
        }

        this.channelFuture = bootstrap.register().syncUninterruptibly();
    }

    @Override
    public ChannelFuture connectToServer(final SocketAddress serverAddress, final ProtocolVersion targetVersion) {
        this.serverAddress = serverAddress;
        return super.connectToServer(serverAddress, targetVersion);
    }

    protected void initializeRakNet(TransportType transportType, final Bootstrap bootstrap) {
        if (!DatagramChannel.class.isAssignableFrom(transportType.udpClientChannelClass())) {
            throw new IllegalArgumentException("Channel type must be a DatagramChannel");
        }
        if (transportType == TransportType.KQUEUE) {
            transportType = TransportType.NIO; // KQueue doesn't work due to requiring the channel to be bound instead of connected
        }

        final RakChannelFactory<RakClientChannel> channelFactory = RakChannelFactory.client((Class<? extends DatagramChannel>) transportType.udpClientChannelClass());
        bootstrap
                .group(EventLoops.getClientEventLoop(transportType))
                .channelFactory(() -> {
                    final Channel channel = channelFactory.newChannel();
                    if (channel.config().setOption(RakChannelOption.RAK_IP_DONT_FRAGMENT, true)) {
                        channel.config().setOption(RakChannelOption.RAK_MTU_SIZES, new Integer[]{1492, 1200, 576});
                    }
                    return channel;
                })
                .option(RakChannelOption.RAK_PROTOCOL_VERSION, ProtocolConstants.BEDROCK_RAKNET_PROTOCOL_VERSION)
                .option(RakChannelOption.RAK_COMPATIBILITY_MODE, true)
                .option(RakChannelOption.RAK_CLIENT_INTERNAL_ADDRESSES, 20)
                .option(RakChannelOption.RAK_TIME_BETWEEN_SEND_CONNECTION_ATTEMPTS_MS, 500)
                .option(RakChannelOption.RAK_CONNECT_TIMEOUT, (long) ViaProxy.getConfig().getConnectTimeout())
                .option(RakChannelOption.RAK_SESSION_TIMEOUT, 30_000L)
                .option(RakChannelOption.RAK_GUID, ThreadLocalRandom.current().nextLong());
    }

    protected void initializeNetherNet(final TransportType transportType, final Bootstrap bootstrap) {
        final NetherNetClientSignaling netherNetSignaling;
        if (this.serverAddress instanceof NetherNetHttpAddress address) {
            if (this.getUserOptions().account() instanceof BedrockAccount bedrockAccount) {
                final BedrockAuthManager authManager = bedrockAccount.getAuthManager();
                final MinecraftMultiplayerToken multiplayerToken = authManager.getMinecraftMultiplayerToken().getUpToDateUnchecked();
                netherNetSignaling = new NetherNetHttpSignaling(EventLoops.getClientEventLoop(TransportType.NIO).next(), address, new NetherNetHttpSignaling.Identity(
                        authManager.getSessionKeyPair(), multiplayerToken.getXuid(), multiplayerToken.getDisplayName(), "example.com"
                ));
            } else {
                this.kickClient("§cOffline mode is currently not supported for NetherNet HTTP signaling");
                return;
            }
        } else if (this.serverAddress instanceof NetherNetLanAddress) {
            netherNetSignaling = new NetherNetDiscoverySignaling();
        } else if (this.serverAddress instanceof NetherNetXboxAddress || this.serverAddress instanceof NetherNetXboxJsonRpcAddress) {
            if (this.getUserOptions().account() instanceof BedrockAccount bedrockAccount) {
                if (this.serverAddress instanceof NetherNetXboxAddress) {
                    netherNetSignaling = new NetherNetXboxSignaling(bedrockAccount.getAuthManager().getMinecraftSession().getUpToDateUnchecked().getAuthorizationHeader());
                } else if (this.serverAddress instanceof NetherNetXboxJsonRpcAddress) {
                    netherNetSignaling = new NetherNetXboxRpcSignaling(bedrockAccount.getAuthManager().getMinecraftSession().getUpToDateUnchecked().getAuthorizationHeader());
                } else {
                    throw new IllegalStateException("Unsupported NetherNet address type: " + this.serverAddress.getClass().getName());
                }
            } else {
                this.kickClient("§cThe configured target server requires Xbox signaling, but no Minecraft: Bedrock Edition account is selected.");
                return;
            }
        } else {
            throw new IllegalStateException("Unsupported NetherNet address type: " + this.serverAddress.getClass().getName());
        }
        final ChannelHandler channelHandler = bootstrap.config().handler();
        bootstrap
                .group(EventLoops.getClientEventLoop(TransportType.NIO))
                .channelFactory(NetherNetChannelFactory.client(netherNetSignaling))
                .option(NetherChannelOption.NETHER_CLIENT_HANDSHAKE_TIMEOUT_MS, ViaProxy.getConfig().getConnectTimeout())
                .option(NetherChannelOption.NETHER_CLIENT_MAX_HANDSHAKE_ATTEMPTS, 1)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(final Channel channel) {
                        channel.pipeline().addLast(channelHandler);
                        channel.pipeline().remove(MessageCodec.NAME);
                    }
                });
    }

    /**
     * Copyright <a href="https://github.com/WaterdogPE/WaterdogPE/blob/b8e8b2e7fac100fd798b685cfae456aa111cd6a3/src/main/java/dev/waterdog/waterdogpe/network/nethernet/HttpClientSignaling.java">WaterdogPE</a>.<br>
     * Licensed under the <a href="https://github.com/WaterdogPE/WaterdogPE/blob/b8e8b2e7fac100fd798b685cfae456aa111cd6a3/LICENSE">GPL</a> license.
     */
    private static final class NetherNetHttpSignaling implements NetherNetClientSignaling {

        private static final long CANDIDATE_QUIET_MILLIS = 700;
        private static final long GATHER_CAP_MILLIS = 5_000;
        private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);

        private static final HttpClient HTTP = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        private final String localNetworkId = Long.toUnsignedString(ThreadLocalRandom.current().nextLong());
        private final List<String> candidates = new ObjectArrayList<>();
        private final EventLoop eventLoop;
        private final InetSocketAddress address;
        private final Identity identity;

        private volatile SignalHandler handler;
        private volatile NotFoundHandler notFound;
        private volatile boolean closed;

        private String offer;
        private long connectionId;
        private ScheduledFuture<?> quiet;
        private ScheduledFuture<?> deadline;
        private boolean sent;

        public NetherNetHttpSignaling(final EventLoop eventLoop, final InetSocketAddress address, final Identity identity) {
            this.eventLoop = eventLoop;
            this.address = address;
            this.identity = identity;
        }

        public record Identity(KeyPair keyPair, String xuid, String name, String domain) {
        }

        @Override
        public CompletableFuture<List<IceServerInfo>> connect(final SocketAddress remoteAddress) {
            return CompletableFuture.completedFuture(List.of());
        }

        @Override
        public void sendSignal(final String targetNetworkId, final String data) {
            final String[] parts = data.split(" ", 3);
            if (parts.length < 3) {
                return;
            }
            final long id;
            try {
                id = Long.parseUnsignedLong(parts[1]);
            } catch (final NumberFormatException e) {
                return;
            }
            final String type = parts[0];
            final String payload = parts[2];
            this.eventLoop.execute(() -> this.onSignal(id, type, payload));
        }

        private void onSignal(final long id, final String type, final String payload) {
            if (this.closed || this.sent) {
                return;
            }
            switch (type) {
                case NetherNetConstants.RTC_NEGOTIATION_CONNECT_REQUEST -> {
                    this.connectionId = id;
                    this.offer = payload;
                    this.deadline = this.eventLoop.schedule(this::post, GATHER_CAP_MILLIS, TimeUnit.MILLISECONDS);
                    this.rearm();
                }
                case NetherNetConstants.RTC_NEGOTIATION_CANDIDATE_ADD -> {
                    this.candidates.add(payload);
                    this.rearm();
                }
            }
        }

        /**
         * Silence is the only completion cue this bus offers, there is no gathering complete signal.
         */
        private void rearm() {
            if (this.offer == null) {
                return;
            }
            if (this.quiet != null) {
                this.quiet.cancel(false);
            }
            this.quiet = this.eventLoop.schedule(this::post, CANDIDATE_QUIET_MILLIS, TimeUnit.MILLISECONDS);
        }

        private void post() {
            if (this.closed || this.sent || this.offer == null) {
                return;
            }
            this.sent = true;
            this.cancelTimers();

            String body = BedrockProxyConnection.SdpUtil.withCandidates(this.offer, this.candidates);
            if (this.identity != null) {
                try {
                    // Signed over the finished offer, so every fingerprint it binds to is already in it.
                    body = BedrockProxyConnection.SdpUtil.withIdentity(body, ClientAssertionFactory.create(body, this.identity.keyPair(),
                            this.identity.xuid(), this.identity.name(), this.identity.domain()));
                } catch (Exception e) {
                    this.fail("unable to sign the identity assertion: " + e);
                    return;
                }
            }
            final HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl(this.address) + "/v1/join/" + this.localNetworkId))
                    .timeout(HTTP_TIMEOUT)
                    .header("Content-Type", "application/sdp")
                    .header("Accept", "application/sdp")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenCompleteAsync(this::onAnswer, this.eventLoop);
        }

        private void onAnswer(final HttpResponse<String> response, final Throwable error) {
            if (this.closed) {
                return;
            }
            if (error != null) {
                this.fail("signaling request failed: " + error.getMessage());
                return;
            }
            if (response.statusCode() / 100 != 2) {
                this.fail("signaling returned HTTP " + response.statusCode());
                return;
            }

            final String answer = response.body();
            // A rejection can arrive as a 2xx with a short body instead of an SDP, so the shape has to
            // be checked rather than the status alone.
            if (answer == null || !answer.startsWith("v=")) {
                this.fail("signaling returned a 2xx that is not an SDP answer: "
                        + (answer == null ? "empty" : answer.strip()));
                return;
            }

            if (this.handler != null) {
                this.handler.onSignal(NetherNetConstants.buildSignalConnectResponse(this.connectionId, answer));
            }
        }

        private void fail(String reason) {
            if (this.notFound != null) {
                this.notFound.onNotFound(reason);
            }
        }

        private void cancelTimers() {
            if (this.quiet != null) {
                this.quiet.cancel(false);
            }
            if (this.deadline != null) {
                this.deadline.cancel(false);
            }
        }

        @Override
        public void setSignalHandler(final long connectionId, final SignalHandler handler) {
            this.handler = handler;
        }

        @Override
        public void removeSignalHandler(final long connectionId) {
            this.handler = null;
        }

        @Override
        public void setNotFoundHandler(final NotFoundHandler handler) {
            this.notFound = handler;
        }

        @Override
        public String getLocalNetworkId() {
            return this.localNetworkId;
        }


        @Override
        public boolean isActive() {
            return !this.closed;
        }

        @Override
        public void close() {
            this.closed = true;
            if (this.eventLoop.inEventLoop()) {
                this.cancelTimers();
            } else {
                this.eventLoop.execute(this::cancelTimers);
            }
        }

        private static String baseUrl(final InetSocketAddress address) {
            final InetAddress host = address.getAddress();
            final String literal = host == null ? address.getHostString() : host.getHostAddress();
            return "http://" + (literal.indexOf(':') >= 0 ? "[" + literal + "]" : literal) + ":" + address.getPort();
        }

    }

    /**
     * Copyright <a href="https://github.com/WaterdogPE/WaterdogPE/blob/b8e8b2e7fac100fd798b685cfae456aa111cd6a3/src/main/java/dev/waterdog/waterdogpe/network/nethernet/SdpUtil.java">WaterdogPE</a>.<br>
     * Licensed under the <a href="https://github.com/WaterdogPE/WaterdogPE/blob/b8e8b2e7fac100fd798b685cfae456aa111cd6a3/LICENSE">GPL</a> license.
     */
    private static final class SdpUtil {

        private static final String IDENTITY = "a=identity:";

        private SdpUtil() {
        }

        /**
         * Inserts an identity assertion as a session level attribute, ahead of the first media line
         * where both the guide and real clients put it.
         */
        public static String withIdentity(String sdp, String value) {
            String eol = eol(sdp);
            StringBuilder out = new StringBuilder(sdp.length() + value.length() + 16);
            boolean inserted = false;
            for (String line : lines(sdp)) {
                if (line.isEmpty()) {
                    continue;
                }
                if (!inserted && line.startsWith("m=")) {
                    out.append(IDENTITY).append(value).append(eol);
                    inserted = true;
                }
                out.append(line).append(eol);
            }
            return out.toString();
        }

        /**
         * Folds trickled candidates into the answer. HTTP signaling is a single round trip, so every
         * candidate has to travel in the answer body. Fingerprint lines are untouched, which is why
         * this does not invalidate the identity assertion.
         */
        public static String withCandidates(String answer, List<String> candidates) {
            String eol = eol(answer);
            StringBuilder out = new StringBuilder(answer.length() + candidates.size() * 96);
            for (String line : lines(answer)) {
                if (!line.isEmpty()) {
                    out.append(line).append(eol);
                }
            }
            for (String candidate : candidates) {
                out.append("a=").append(candidate.trim()).append(eol);
            }
            return out.append("a=end-of-candidates").append(eol).toString();
        }

        private static String[] lines(String sdp) {
            return sdp.split("\r\n|\n", -1);
        }

        private static String eol(String sdp) {
            return sdp.contains("\r\n") ? "\r\n" : "\n";
        }

    }

    /**
     * Copyright <a href="https://github.com/WaterdogPE/WaterdogPE/blob/b8e8b2e7fac100fd798b685cfae456aa111cd6a3/src/main/java/dev/waterdog/waterdogpe/network/nethernet/ClientAssertionFactory.java">WaterdogPE</a>.<br>
     * Licensed under the <a href="https://github.com/WaterdogPE/WaterdogPE/blob/b8e8b2e7fac100fd798b685cfae456aa111cd6a3/LICENSE">GPL</a> license.
     */
    private static final class ClientAssertionFactory {

        private static final long LIFETIME_SECONDS = 3600;

        private ClientAssertionFactory() {
        }

        /**
         * @param offerSdp the offer whose fingerprints the assertion binds to, without an identity line
         * @param xuid     the connecting player's XUID, surfaced to the downstream server
         * @param name     the connecting player's name
         */
        public static String create(String offerSdp, KeyPair keyPair, String xuid, String name, String domain) {
            ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
            String cpk = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

            Instant now = Instant.now();

            String token = Jwts.builder()
                    .claim("cpk", cpk)
                    .claim("xid", xuid == null ? "" : xuid)
                    .claim("xname", name == null ? "" : name)
                    .issuer(domain)
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plusSeconds(LIFETIME_SECONDS)))
                    .signWith(privateKey, Jwts.SIG.ES384)
                    .compact();

            String fingerprintJws = Jwts.builder()
                    .content(IdentityUtils.getCanonicalFingerprintJson(offerSdp)
                            .getBytes(StandardCharsets.UTF_8))
                    .signWith(privateKey, Jwts.SIG.ES384)
                    .compact();

            String[] parts = fingerprintJws.split("\\.");
            String detachedFingerprintJws = parts[0] + ".." + parts[2];

            JsonObject assertion = new JsonObject();
            assertion.addProperty("fingerprints", detachedFingerprintJws);
            assertion.addProperty("token", token);

            JsonObject idp = new JsonObject();
            idp.addProperty("domain", domain);
            idp.addProperty("protocol", "default");

            JsonObject envelope = new JsonObject();
            envelope.addProperty("assertion", assertion.toString());
            envelope.add("idp", idp);

            return Base64.getEncoder().encodeToString(envelope.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

}
