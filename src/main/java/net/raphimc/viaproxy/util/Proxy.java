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
package net.raphimc.viaproxy.util;

import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import net.lenni0451.commons.httpclient.proxy.ProxyType;
import net.lenni0451.commons.httpclient.utils.URLWrapper;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Locale;

public class Proxy {

    private final String protocol;
    private final SocketAddress address;
    private final String username;
    private final String password;

    public Proxy(final URI uri) {
        this(uri.getScheme(), new InetSocketAddress(uri.getHost(), uri.getPort()), uri.getUserInfo());
    }

    public Proxy(final String protocol, final SocketAddress address) {
        this(protocol, address, null, null);
    }

    public Proxy(final String protocol, final SocketAddress address, final String userInfo) {
        this(protocol, address, userInfo != null ? userInfo.split(":", 2)[0] : null, userInfo != null ? (userInfo.split(":", 2).length > 1 ? userInfo.split(":", 2)[1] : null) : null);
    }

    public Proxy(final String protocol, final SocketAddress address, final String username, final String password) {
        this.protocol = protocol.toUpperCase(Locale.ROOT);
        this.address = address;
        this.username = username;
        this.password = password;
    }

    public ProxyHandler createNettyProxyHandler() {
        switch (this.protocol) {
            case "HTTP", "HTTPS" -> {
                if (this.username != null && this.password != null) return new HttpProxyHandler(this.address, this.username, this.password);
                else return new HttpProxyHandler(this.address);
            }
            case "SOCKS4" -> {
                if (this.username != null) return new Socks4ProxyHandler(this.address, this.username);
                else return new Socks4ProxyHandler(this.address);
            }
            case "SOCKS5" -> {
                if (this.username != null && this.password != null) return new Socks5ProxyHandler(this.address, this.username, this.password);
                else return new Socks5ProxyHandler(this.address);
            }
            default -> throw new IllegalArgumentException("Unsupported proxy protocol: " + this.protocol);
        }
    }

    public java.net.Proxy toJavaProxy() {
        final ProxyType proxyType = switch (this.protocol) {
            case "HTTP", "HTTPS" -> ProxyType.HTTP;
            case "SOCKS4" -> ProxyType.SOCKS4;
            case "SOCKS5" -> ProxyType.SOCKS5;
            default -> throw new IllegalArgumentException("Unsupported proxy protocol: " + this.protocol);
        };
        return new net.lenni0451.commons.httpclient.proxy.ProxyHandler(proxyType, this.address, this.username, this.password).toJavaProxy();
    }

    public URI toURI() {
        String userInfo = null;
        if (this.username != null) {
            userInfo = this.username;
            if (this.password != null) {
                userInfo += ":" + this.password;
            }
        }
        return URLWrapper.empty().setProtocol(this.protocol.toLowerCase(Locale.ROOT)).setHost(AddressUtil.toString(this.address)).setUserInfo(userInfo).toURI();
    }

    public String getProtocol() {
        return this.protocol;
    }

    public SocketAddress getAddress() {
        return this.address;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

}
