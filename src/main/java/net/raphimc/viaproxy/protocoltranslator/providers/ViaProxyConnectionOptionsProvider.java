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

import com.viaversion.viabackwards.protocol.v26_4to26_3.provider.ConnectionOptionsProvider;
import com.viaversion.viaversion.api.connection.UserConnection;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

import java.security.GeneralSecurityException;

public class ViaProxyConnectionOptionsProvider implements ConnectionOptionsProvider {

    @Override
    public void applyOutbound(final UserConnection connection, final byte options, final int compressionThreshold) {
        if ((options & ENCRYPTION) != 0) {
            try {
                ProxyConnection.fromUserConnection(connection).enablePreNettyEncryption();
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("Failed to enable encryption", e);
            }
        }
        if ((options & COMPRESSION) != 0) {
            connection.getChannel().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).set(compressionThreshold);
        }
    }

}
