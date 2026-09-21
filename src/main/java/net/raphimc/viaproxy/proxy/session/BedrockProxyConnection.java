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
import io.netty.util.concurrent.DefaultThreadFactory;
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
import net.raphimc.viaproxy.util.logging.Logger;
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
import tel.schich.libdatachannel.LibDataChannelArchDetect;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
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
        LibDataChannelArchDetect.initialize();

        final NetherNetClientSignaling netherNetSignaling;
        if (this.serverAddress instanceof NetherNetHttpAddress address) {
            if (this.getUserOptions().account() instanceof BedrockAccount bedrockAccount) {
                final BedrockAuthManager authManager = bedrockAccount.getAuthManager();
                final MinecraftMultiplayerToken multiplayerToken = authManager.getMinecraftMultiplayerToken().getUpToDateUnchecked();
                netherNetSignaling = new NetherNetHttpSignaling(EventLoops.getClientEventLoop(TransportType.NIO).next(), address, new NetherNetHttpSignaling.Identity(
                        authManager.getSessionKeyPair(), multiplayerToken.getToken()
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
        private static final int HTTP_TIMEOUT_MILLIS = 10_000;
        private static final int MAX_BODY_BYTES = 64 * 1024;

        private static final Executor EXECUTOR = Executors.newCachedThreadPool(new DefaultThreadFactory("NetherNet Signaling", true));
        private static final SSLSocketFactory TRUST_ALL_SSL_FACTORY = createTrustAllSslFactory();
        private static final HostnameVerifier TRUST_ALL_HOSTNAME_VERIFIER = (hostname, session) -> true;

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

        public record Identity(KeyPair keyPair, String token) {
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
                    body = BedrockProxyConnection.SdpUtil.withIdentity(body, ClientAssertionFactory.create(body, this.identity.keyPair(), this.identity.token()));
                } catch (Exception e) {
                    this.fail("unable to sign the identity assertion: " + e);
                    return;
                }
            }
            final String offerSdp = body;
            CompletableFuture.supplyAsync(() -> this.exchange(offerSdp), EXECUTOR).whenCompleteAsync(this::onAnswer, this.eventLoop);
        }

        /**
         * Sends the offer over TLS and falls back to plaintext, the same way the NetherNet status ping picks
         * its scheme, so every server which can be pinged can also be joined.
         */
        private String exchange(final String offerSdp) {
            try {
                try {
                    return this.exchange(offerSdp, "https");
                } catch (SSLException | EOFException | SocketException e) {
                    return this.exchange(offerSdp, "http");
                }
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }

        private String exchange(final String offerSdp, final String scheme) throws IOException {
            final URI uri = URI.create(scheme + "://" + host(this.address) + ":" + this.address.getPort() + "/v1/join/" + this.localNetworkId);
            final HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            if (connection instanceof HttpsURLConnection httpsConnection) {
                httpsConnection.setSSLSocketFactory(TRUST_ALL_SSL_FACTORY);
                httpsConnection.setHostnameVerifier(TRUST_ALL_HOSTNAME_VERIFIER);
            }
            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-Agent", NetherNetConstants.SIGNALING_USER_AGENT);
            connection.setRequestProperty("Content-Type", "application/sdp");
            connection.setRequestProperty("Accept", "application/sdp");
            connection.setConnectTimeout(HTTP_TIMEOUT_MILLIS);
            connection.setReadTimeout(HTTP_TIMEOUT_MILLIS);
            connection.setInstanceFollowRedirects(false);
            connection.setDoOutput(true);
            try {
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(offerSdp.getBytes(StandardCharsets.UTF_8));
                }

                final int responseCode = connection.getResponseCode();
                if ("http".equals(scheme) && requiresTls(connection, responseCode)) {
                    return this.exchange(offerSdp, "https");
                }
                if (responseCode / 100 != 2) {
                    throw new IOException("signaling returned HTTP " + responseCode + " " + read(connection.getErrorStream()).strip());
                }

                final String answer = read(connection.getInputStream());
                // A rejection can arrive as a 2xx with a short body instead of an SDP, so the shape has to
                // be checked rather than the status alone.
                if (!answer.startsWith("v=")) {
                    throw new IOException("signaling returned a 2xx that is not an SDP answer: " + (answer.isEmpty() ? "empty" : answer.strip()));
                }
                return answer;
            } finally {
                connection.disconnect();
            }
        }

        private void onAnswer(final String answer, final Throwable error) {
            if (this.closed) {
                return;
            }
            if (error != null) {
                final Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                this.fail("signaling request failed: " + cause.getMessage());
                return;
            }

            final SignalHandler handler = this.handler;
            if (handler != null) {
                handler.onSignal(NetherNetConstants.buildSignalConnectResponse(this.connectionId, answer));
            }
        }

        private void fail(final String reason) {
            Logger.LOGGER.warn("NetherNet signaling to " + this.address + " failed: " + reason);
            final NotFoundHandler notFound = this.notFound;
            if (notFound != null) {
                notFound.onNotFound(reason);
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

        private static String host(final InetSocketAddress address) {
            final InetAddress host = address.getAddress();
            final String literal = host == null ? address.getHostString() : host.getHostAddress();
            return literal.indexOf(':') >= 0 ? "[" + literal + "]" : literal;
        }

        private static boolean requiresTls(final HttpURLConnection connection, final int responseCode) {
            if (responseCode == 426) {
                return true;
            }
            if (responseCode != 301 && responseCode != 302 && responseCode != 307 && responseCode != 308) {
                return false;
            }
            final String location = connection.getHeaderField("Location");
            return location != null && location.startsWith("https://");
        }

        private static String read(final InputStream inputStream) throws IOException {
            if (inputStream == null) {
                return "";
            }
            try (InputStream stream = inputStream) {
                final byte[] body = stream.readNBytes(MAX_BODY_BYTES + 1);
                if (body.length > MAX_BODY_BYTES) {
                    throw new IOException("signaling body exceeds " + MAX_BODY_BYTES + " bytes");
                }
                return new String(body, StandardCharsets.UTF_8);
            }
        }

        /**
         * NetherNet anchors trust in the identity assertion rather than in the signaling certificate, and
         * servers commonly present a self signed one on a raw address. Rejecting those would push them onto
         * the plaintext path they answer with a 426.
         */
        private static SSLSocketFactory createTrustAllSslFactory() {
            try {
                final SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    @Override
                    public void checkClientTrusted(final X509Certificate[] chain, final String authType) {
                    }

                    @Override
                    public void checkServerTrusted(final X509Certificate[] chain, final String authType) {
                    }
                }}, new SecureRandom());
                return sslContext.getSocketFactory();
            } catch (Throwable e) {
                throw new ExceptionInInitializerError(e);
            }
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

        private static final String IDENTITY_PROVIDER_DOMAIN = "authorization.franchise.minecraft-services.net";

        private ClientAssertionFactory() {
        }

        /**
         * @param offerSdp the offer whose fingerprints the assertion binds to, without an identity line
         * @param keyPair  the key pair the token's cpk claim is bound to
         * @param token    the multiplayer token the Minecraft auth service issued for the account
         */
        public static String create(final String offerSdp, final KeyPair keyPair, final String token) {
            final String fingerprintJws = Jwts.builder()
                    .content(IdentityUtils.getCanonicalFingerprintJson(offerSdp)
                            .getBytes(StandardCharsets.UTF_8))
                    .signWith((ECPrivateKey) keyPair.getPrivate(), Jwts.SIG.ES384)
                    .compact();

            final String[] parts = fingerprintJws.split("\\.");
            final String detachedFingerprintJws = parts[0] + ".." + parts[2];

            final JsonObject assertion = new JsonObject();
            assertion.addProperty("fingerprints", detachedFingerprintJws);
            assertion.addProperty("token", token);

            final JsonObject idp = new JsonObject();
            idp.addProperty("domain", IDENTITY_PROVIDER_DOMAIN);
            idp.addProperty("protocol", "default");

            final JsonObject envelope = new JsonObject();
            envelope.addProperty("assertion", assertion.toString());
            envelope.add("idp", idp);

            return Base64.getEncoder().encodeToString(envelope.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

}
