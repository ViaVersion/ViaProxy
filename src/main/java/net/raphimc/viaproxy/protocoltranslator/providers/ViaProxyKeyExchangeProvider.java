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

import com.viaversion.viabackwards.protocol.v26_4to26_3.provider.KeyExchangeProvider;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.protocols.v26_3to26_4.crypto.SessionKeys26_4;
import net.raphimc.viaproxy.proxy.external_interface.ExternalInterface;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;
import net.raphimc.viaproxy.proxy.util.SplitAESEncryption;

import javax.crypto.Cipher;
import java.security.GeneralSecurityException;

public class ViaProxyKeyExchangeProvider implements KeyExchangeProvider {

    @Override
    public void handleKeyExchange(final UserConnection connection, final KeyExchange keyExchange) {
        final ProxyConnection proxyConnection = ProxyConnection.fromUserConnection(connection);
        final SessionKeys26_4 sessionKeys = keyExchange.sessionKeys();
        if (keyExchange.shouldAuthenticate()) {
            ExternalInterface.joinServer(sessionKeys.authenticationDigest(), proxyConnection);
        }

        keyExchange.sendResponse();

        // Enabled once the server has applied it, see ViaProxyConnectionOptionsProvider
        try {
            proxyConnection.setPendingEncryption(new SplitAESEncryption(sessionKeys.clientboundCipher(Cipher.DECRYPT_MODE), sessionKeys.serverboundCipher(Cipher.ENCRYPT_MODE)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to set up encryption", e);
        }
    }

}
