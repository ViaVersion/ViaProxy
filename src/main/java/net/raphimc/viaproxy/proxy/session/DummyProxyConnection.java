/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
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

import com.mojang.authlib.GameProfile;
import com.viaversion.viaversion.api.connection.UserConnection;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginHelloPacket1_7;
import net.raphimc.netminecraft.util.ServerAddress;
import net.raphimc.vialoader.util.VersionEnum;

import java.security.Key;
import java.util.concurrent.CompletableFuture;

public class DummyProxyConnection extends ProxyConnection {

    public DummyProxyConnection(final Channel c2p) {
        super(null, null, c2p);
    }

    @Override
    public void initialize(Bootstrap bootstrap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void connectToServer(ServerAddress serverAddress, VersionEnum targetVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServerAddress getServerAddress() {
        throw new UnsupportedOperationException();
    }

    @Override
    public VersionEnum getServerVersion() {
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
    public void setGameProfile(GameProfile gameProfile) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GameProfile getGameProfile() {
        return null;
    }

    @Override
    public void setLoginHelloPacket(C2SLoginHelloPacket1_7 loginHelloPacket) {
        throw new UnsupportedOperationException();
    }

    @Override
    public C2SLoginHelloPacket1_7 getLoginHelloPacket() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUserConnection(UserConnection userConnection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UserConnection getUserConnection() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectionState getConnectionState() {
        return ConnectionState.HANDSHAKING;
    }

    @Override
    public CompletableFuture<ByteBuf> sendCustomPayload(String channel, ByteBuf data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean handleCustomPayload(int id, ByteBuf data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setClassicMpPass(String classicMpPass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getClassicMpPass() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isClosed() {
        return false;
    }

}
