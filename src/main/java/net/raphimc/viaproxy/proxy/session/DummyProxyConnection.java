/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2024 RK_01/RaphiMC and contributors
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

import com.google.common.net.HostAndPort;
import com.mojang.authlib.GameProfile;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginHelloPacket;
import net.raphimc.netminecraft.util.ChannelType;

import java.net.SocketAddress;
import java.security.Key;

public class DummyProxyConnection extends ProxyConnection {

    public DummyProxyConnection(final Channel c2p) {
        super(null, null, c2p);
    }

    @Override
    public void initialize(ChannelType channelType, Bootstrap bootstrap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChannelFuture connectToServer(SocketAddress serverAddress, ProtocolVersion targetVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SocketAddress getServerAddress() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProtocolVersion getServerVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public HostAndPort getClientHandshakeAddress() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setClientHandshakeAddress(HostAndPort clientHandshakeAddress) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GameProfile getGameProfile() {
        return null;
    }

    @Override
    public void setGameProfile(GameProfile gameProfile) {
        throw new UnsupportedOperationException();
    }

    @Override
    public C2SLoginHelloPacket getLoginHelloPacket() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLoginHelloPacket(C2SLoginHelloPacket loginHelloPacket) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setKeyForPreNettyEncryption(Key key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void enablePreNettyEncryption() {
        throw new UnsupportedOperationException();
    }

    @Override
    public UserConnection getUserConnection() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUserConnection(UserConnection userConnection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UserOptions getUserOptions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUserOptions(UserOptions userOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectionState getC2pConnectionState() {
        return ConnectionState.HANDSHAKING;
    }

    @Override
    public ConnectionState getP2sConnectionState() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isClosed() {
        return false;
    }

}
