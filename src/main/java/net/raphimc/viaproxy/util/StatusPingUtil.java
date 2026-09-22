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
package net.raphimc.viaproxy.util;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.lenni0451.mcping.MCPing;
import net.lenni0451.mcping.pings.sockets.impl.factories.SocketChannelSocketFactory;
import net.lenni0451.mcping.responses.BedrockNethernetPingResponse;
import net.lenni0451.mcping.responses.BedrockRaknetPingResponse;
import net.lenni0451.mcping.responses.IPingResponse;
import net.lenni0451.mcping.responses.MCPingResponse;
import net.raphimc.viaproxy.ViaProxy;

import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class StatusPingUtil {

    public static CompletableFuture<ProtocolVersion> detectProtocolVersion(final SocketAddress serverAddress, final ProtocolVersion clientVersion) {
        return MCPing
                .pingModern(clientVersion.getOriginalVersion(), true)
                .tcpSocketFactory(new SocketChannelSocketFactory())
                .address(AddressUtil.toJ16UnixSocketAddress(serverAddress))
                .noResolve()
                .timeout(ViaProxy.getConfig().getConnectTimeout() / 2, ViaProxy.getConfig().getConnectTimeout() / 2)
                .getAsync()
                .thenApply(response -> {
                    if (response.version.protocol == clientVersion.getOriginalVersion()) { // If the server is on the same version as the client, we can just connect
                        return clientVersion;
                    }

                    if (ProtocolVersion.isRegistered(response.version.protocol)) { // If the protocol is registered, we can use it
                        return ProtocolVersion.getProtocol(response.version.protocol);
                    } else {
                        for (ProtocolVersion protocolVersion : ProtocolVersion.getReversedProtocols()) {
                            for (String version : protocolVersion.getIncludedVersions()) {
                                if (response.version.name.contains(version)) {
                                    return protocolVersion;
                                }
                            }
                        }
                        throw new RuntimeException("Unable to detect the server version\nServer sent an invalid protocol id: " + response.version.protocol + " (" + response.version.name + "§r)");
                    }
                });
    }

    public static CompletableFuture<BedrockRaknetPingResponse> pingBedrockRakNet(final SocketAddress serverAddress) {
        return MCPing
                .pingBedrockRaknet()
                .address(AddressUtil.toJ16UnixSocketAddress(serverAddress))
                .noResolve()
                .timeout(ViaProxy.getConfig().getConnectTimeout() / 2, ViaProxy.getConfig().getConnectTimeout() / 2)
                .getAsync();
    }

    public static CompletableFuture<BedrockNethernetPingResponse> pingBedrockNetherNetHttp(final SocketAddress serverAddress) {
        return MCPing
                .pingBedrockNethernet()
                .address(AddressUtil.toJ16UnixSocketAddress(serverAddress))
                .noResolve()
                .timeout(ViaProxy.getConfig().getConnectTimeout() / 2, ViaProxy.getConfig().getConnectTimeout() / 2)
                .getAsync();
    }

    public static MCPingResponse convertToJavaPingResponse(final IPingResponse response) {
        if (response instanceof MCPingResponse javaResponse) {
            return javaResponse;
        }
        final MCPingResponse javaResponse = new MCPingResponse();
        javaResponse.description = response.getMotd();
        javaResponse.version = new MCPingResponse.Version();
        javaResponse.version.name = response.getVersionName();
        javaResponse.version.protocol = response.getProtocolId();
        javaResponse.players = new MCPingResponse.Players();
        javaResponse.players.online = response.getOnlinePlayers();
        javaResponse.players.max = response.getMaxPlayers();
        javaResponse.players.sample = Stream.concat(response.getSample().stream(), Stream.of("Ping: " + response.getPing() + "ms")).map(sample -> {
            final MCPingResponse.Players.Player player = new MCPingResponse.Players.Player();
            player.name = sample;
            player.id = "00000000-0000-0000-0000-000000000000";
            return player;
        }).toArray(MCPingResponse.Players.Player[]::new);
        if (response instanceof BedrockRaknetPingResponse bedrockResponse) {
            javaResponse.version.name = Objects.requireNonNullElse(bedrockResponse.gameId, "") + " " + response.getVersionName();
        }
        return javaResponse;
    }

}
