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

import com.google.common.hash.Hashing;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.handler.ThrowingResponseHandler;
import net.lenni0451.commons.httpclient.requests.impl.GetRequest;
import net.raphimc.vialegacy.protocol.classic.c0_28_30toa1_0_15.provider.ClassicMPPassProvider;
import net.raphimc.vialegacy.protocol.release.r1_2_4_5tor1_3_1_2.provider.OldAuthProvider;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.logging.Level;

public class ViaProxyClassicMPPassProvider extends ClassicMPPassProvider {

    @Override
    public String getMpPass(UserConnection user) {
        final String mppass = ProxyConnection.fromUserConnection(user).getUserOptions().classicMpPass();
        if (mppass != null && !mppass.isBlank()) {
            return mppass;
        } else if (ViaProxy.getConfig().useBetacraftAuth()) {
            try {
                final HttpClient httpClient = new HttpClient();
                String externalIp;
                try {
                    externalIp = httpClient.execute(new GetRequest("https://checkip.amazonaws.com"), new ThrowingResponseHandler()).getContentAsString();
                } catch (Throwable e) {
                    throw new RuntimeException("Failed to get external IP address!", e);
                }
                final byte[] hash = Hashing.sha1().hashString(externalIp.strip(), StandardCharsets.UTF_8).asBytes();
                Via.getManager().getProviders().get(OldAuthProvider.class).sendAuthRequest(user, HexFormat.of().formatHex(hash));
            } catch (Throwable e) {
                Via.getPlatform().getLogger().log(Level.WARNING, "An unknown error occurred while authenticating with BetaCraft", e);
            }
        }

        return super.getMpPass(user);
    }

}
