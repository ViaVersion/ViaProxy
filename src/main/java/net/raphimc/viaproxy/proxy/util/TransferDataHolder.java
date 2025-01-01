/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2025 RK_01/RaphiMC and contributors
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
package net.raphimc.viaproxy.proxy.util;

import com.google.common.cache.CacheBuilder;
import com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.storage.CookieStorage;
import io.netty.channel.Channel;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TransferDataHolder {

    private static final Map<InetAddress, InetSocketAddress> TEMP_REDIRECTS = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).<InetAddress, InetSocketAddress>build().asMap();
    private static final Map<InetAddress, CookieStorage> COOKIE_STORAGES = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).<InetAddress, CookieStorage>build().asMap();

    public static void addTempRedirect(final Channel channel, final InetSocketAddress redirect) {
        TEMP_REDIRECTS.put(getChannelAddress(channel), redirect);
    }

    public static void addCookieStorage(final Channel channel, final CookieStorage cookieStorage) {
        COOKIE_STORAGES.put(getChannelAddress(channel), cookieStorage);
    }

    public static InetSocketAddress removeTempRedirect(final Channel channel) {
        return TEMP_REDIRECTS.remove(getChannelAddress(channel));
    }

    public static CookieStorage removeCookieStorage(final Channel channel) {
        return COOKIE_STORAGES.remove(getChannelAddress(channel));
    }

    public static boolean hasTempRedirect(final Channel channel) {
        return TEMP_REDIRECTS.containsKey(getChannelAddress(channel));
    }

    public static boolean hasCookieStorage(final Channel channel) {
        return COOKIE_STORAGES.containsKey(getChannelAddress(channel));
    }

    private static InetAddress getChannelAddress(final Channel channel) {
        return ((InetSocketAddress) channel.remoteAddress()).getAddress();
    }

}
