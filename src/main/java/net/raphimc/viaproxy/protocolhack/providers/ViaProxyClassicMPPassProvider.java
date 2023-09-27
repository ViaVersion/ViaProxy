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
package net.raphimc.viaproxy.protocolhack.providers;

import com.google.common.hash.Hashing;
import com.google.common.io.Resources;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import net.raphimc.vialegacy.protocols.classic.protocola1_0_15toc0_28_30.providers.ClassicMPPassProvider;
import net.raphimc.vialegacy.protocols.release.protocol1_3_1_2to1_2_4_5.providers.OldAuthProvider;
import net.raphimc.vialegacy.protocols.release.protocol1_7_2_5to1_6_4.storage.HandshakeStorage;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class ViaProxyClassicMPPassProvider extends ClassicMPPassProvider {

    @Override
    public String getMpPass(UserConnection user) {
        final String mppass = ProxyConnection.fromUserConnection(user).getUserOptions().classicMpPass();
        if (mppass != null && !mppass.isEmpty() && !mppass.equals("0")) {
            return mppass;
        } else if (Options.BETACRAFT_AUTH) {
            final HandshakeStorage handshakeStorage = user.get(HandshakeStorage.class);
            return getBetacraftMpPass(user, user.getProtocolInfo().getUsername(), handshakeStorage.getHostname(), handshakeStorage.getPort());
        } else {
            return super.getMpPass(user);
        }
    }

    private static String getBetacraftMpPass(final UserConnection user, final String username, final String serverIp, final int port) {
        try {
            final String server = InetAddress.getByName(serverIp).getHostAddress() + ":" + port;
            Via.getManager().getProviders().get(OldAuthProvider.class).sendAuthRequest(user, Hashing.sha1().hashBytes(server.getBytes()).toString());
            final String mppass = Resources.toString(new URL("http://api.betacraft.uk/getmppass.jsp?user=" + username + "&server=" + server), StandardCharsets.UTF_8);
            if (mppass.contains("FAILED") || mppass.contains("SERVER NOT FOUND")) return "0";
            return mppass;
        } catch (Throwable e) {
            Via.getPlatform().getLogger().log(Level.WARNING, "An unknown error occurred while authenticating with BetaCraft", e);
        }
        return "0";
    }

}
