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
import net.raphimc.vialegacy.protocol.release.r1_2_4_5tor1_3_1_2.provider.OldAuthProvider;
import net.raphimc.viaproxy.proxy.external_interface.ExternalInterface;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

public class ViaProxyOldAuthProvider extends OldAuthProvider {

    @Override
    public void sendAuthRequest(final UserConnection user, final String serverId) throws Throwable {
        ExternalInterface.joinServer(serverId, ProxyConnection.fromUserConnection(user));
    }

}
