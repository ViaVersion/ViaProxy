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
package net.raphimc.viaproxy.protocoltranslator.impl;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.libs.gson.JsonArray;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.platform.UserConnectionViaVersionPlatform;
import io.netty.channel.ChannelFutureListener;
import net.raphimc.netminecraft.packet.impl.configuration.C2SConfigCustomPayloadPacket;
import net.raphimc.netminecraft.packet.impl.configuration.S2CConfigCustomPayloadPacket;
import net.raphimc.netminecraft.packet.impl.play.C2SPlayCustomPayloadPacket;
import net.raphimc.netminecraft.packet.impl.play.S2CPlayCustomPayloadPacket;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.plugins.ViaProxyPlugin;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;
import net.raphimc.viaproxy.proxy.util.CloseAndReturn;
import net.raphimc.viaproxy.util.logging.JLoggerToSLF4J;
import org.slf4j.LoggerFactory;

import java.util.logging.Logger;

public class ViaProxyViaVersionPlatform extends UserConnectionViaVersionPlatform {

    public ViaProxyViaVersionPlatform() {
        super(ViaProxy.getCwd());
    }

    @Override
    public Logger createLogger(final String name) {
        return new JLoggerToSLF4J(LoggerFactory.getLogger(name));
    }

    @Override
    public String getPlatformName() {
        return "ViaProxy";
    }

    @Override
    public String getPlatformVersion() {
        return ViaProxy.VERSION;
    }

    @Override
    public boolean isProxy() {
        return true;
    }

    @Override
    public boolean kickPlayer(final UserConnection connection, final String message) {
        try {
            ProxyConnection.fromUserConnection(connection).kickClient(message);
        } catch (CloseAndReturn ignored) {
        }
        return true;
    }

    @Override
    public void sendCustomPayload(final UserConnection connection, final String channel, final byte[] message) {
        final ProxyConnection proxyConnection = ProxyConnection.fromUserConnection(connection);
        proxyConnection.getChannel().writeAndFlush(switch (proxyConnection.getP2sConnectionState()) {
            case CONFIGURATION -> new C2SConfigCustomPayloadPacket(channel, message);
            case PLAY -> new C2SPlayCustomPayloadPacket(channel, message);
            default -> throw new UnsupportedOperationException("Can't send custom payloads in state: " + proxyConnection.getP2sConnectionState());
        }).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    @Override
    public void sendCustomPayloadToClient(final UserConnection connection, final String channel, final byte[] message) {
        final ProxyConnection proxyConnection = ProxyConnection.fromUserConnection(connection);
        proxyConnection.getC2P().writeAndFlush(switch (proxyConnection.getC2pConnectionState()) {
            case CONFIGURATION -> new S2CConfigCustomPayloadPacket(channel, message);
            case PLAY -> new S2CPlayCustomPayloadPacket(channel, message);
            default -> throw new UnsupportedOperationException("Can't send custom payloads in state: " + proxyConnection.getC2pConnectionState());
        }).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    @Override
    public JsonObject getDump() {
        final JsonObject root = new JsonObject();

        root.addProperty("impl_version", ViaProxy.IMPL_VERSION);

        final JsonArray plugins = new JsonArray();
        for (ViaProxyPlugin plugin : ViaProxy.getPluginManager().getPlugins()) {
            final JsonObject pluginObj = new JsonObject();
            pluginObj.addProperty("name", plugin.getName());
            pluginObj.addProperty("version", plugin.getVersion());
            pluginObj.addProperty("author", plugin.getAuthor());
            plugins.add(pluginObj);
        }
        root.add("plugins", plugins);

        return root;
    }

}
