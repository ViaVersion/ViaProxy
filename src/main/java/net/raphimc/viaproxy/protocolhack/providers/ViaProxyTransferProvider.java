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
package net.raphimc.viaproxy.protocolhack.providers;

import com.viaversion.viaversion.api.connection.UserConnection;
import net.raphimc.viabedrock.protocol.providers.TransferProvider;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;
import net.raphimc.viaproxy.proxy.util.CloseAndReturn;
import net.raphimc.viaproxy.proxy.util.TransferDataHolder;

import java.net.InetSocketAddress;

public class ViaProxyTransferProvider extends TransferProvider {

    @Override
    public void connectToServer(UserConnection user, InetSocketAddress newAddress) {
        TransferDataHolder.addTempRedirect(ProxyConnection.fromChannel(user.getChannel()).getC2P(), newAddress);
        try {
            ProxyConnection.fromUserConnection(user).kickClient("§aThe server transferred you to another server §7(§e" + newAddress.getHostName() + ":" + newAddress.getPort() + "§7)§a. Please reconnect to ViaProxy.");
        } catch (CloseAndReturn ignored) {
        }
    }

}
