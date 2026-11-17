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
package net.raphimc.viaproxy.protocoltranslator.providers;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.protocols.v26_3to26_4.provider.LegacyKeyExchangeProvider;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.crypto.AESEncryption;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import net.raphimc.vialegacy.protocol.release.r1_6_4tor1_7_2_5.storage.ProtocolMetadataStorage;
import net.raphimc.viaproxy.proxy.external_interface.ExternalInterface;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

import java.security.GeneralSecurityException;

public class ViaProxyLegacyKeyExchangeProvider implements LegacyKeyExchangeProvider {

    @Override
    public void handleKeyExchange(final UserConnection connection, final LegacyKeyExchange keyExchange) {
        final ProxyConnection proxyConnection = ProxyConnection.fromUserConnection(connection);
        boolean auth = keyExchange.shouldAuthenticate();
        if (auth && proxyConnection.getServerVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_6_4)) {
            auth = connection.get(ProtocolMetadataStorage.class).isAuthenticate();
        }
        if (auth) {
            ExternalInterface.joinServer(keyExchange.serverHash(), proxyConnection);
        }

        keyExchange.sendResponse();

        // Old servers enable encryption right after receiving the key
        if (proxyConnection.getServerVersion().newerThanOrEqualTo(ProtocolVersion.v1_7_2)) {
            try {
                proxyConnection.getChannel().attr(MCPipeline.ENCRYPTION_ATTRIBUTE_KEY).set(new AESEncryption(keyExchange.secretKey()));
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("Failed to enable encryption", e);
            }
        } else {
            proxyConnection.setKeyForPreNettyEncryption(keyExchange.secretKey());
        }
    }

}
