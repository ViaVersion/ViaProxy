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
package net.raphimc.viaproxy.protocoltranslator.providers;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionType;
import com.viaversion.viaversion.protocol.RedirectProtocolVersion;
import com.viaversion.viaversion.protocol.version.BaseVersionProvider;
import net.raphimc.netminecraft.constants.MCVersion;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

public class ViaProxyVersionProvider extends BaseVersionProvider {

    @Override
    public ProtocolVersion getClientProtocol(UserConnection connection) {
        final ProtocolVersion clientProtocol = connection.getProtocolInfo().protocolVersion();
        if (!clientProtocol.isKnown() && ProtocolVersion.isRegistered(VersionType.SPECIAL, clientProtocol.getOriginalVersion())) {
            return ProtocolVersion.getProtocol(VersionType.SPECIAL, clientProtocol.getOriginalVersion());
        } else {
            return super.getClientProtocol(connection);
        }
    }

    @Override
    public ProtocolVersion getClosestServerProtocol(UserConnection connection) {
        final ProtocolVersion clientProtocol = connection.getProtocolInfo().protocolVersion();
        if (connection.isClientSide()) {
            return ProxyConnection.fromUserConnection(connection).getServerVersion();
        } else if (clientProtocol.getVersionType() == VersionType.RELEASE) {
            if (MCVersion.ALL_VERSIONS.containsKey(clientProtocol.getVersion())) {
                return clientProtocol;
            } else { // Version not supported by NetMinecraft
                return MCVersion.ALL_VERSIONS.keySet().stream().min((o1, o2) -> {
                    final int diff1 = Math.abs(o1 - clientProtocol.getVersion());
                    final int diff2 = Math.abs(o2 - clientProtocol.getVersion());
                    return Integer.compare(diff1, diff2);
                }).map(ProtocolVersion::getProtocol).orElse(ProtocolVersion.unknown);
            }
        } else if (clientProtocol instanceof RedirectProtocolVersion redirectProtocolVersion) {
            return redirectProtocolVersion.getOrigin();
        } else {
            return ProtocolVersion.v1_7_2;
        }
    }

}
