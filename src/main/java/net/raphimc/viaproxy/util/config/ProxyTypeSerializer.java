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
package net.raphimc.viaproxy.util.config;

import net.lenni0451.optconfig.serializer.ConfigTypeSerializer;
import net.raphimc.viaproxy.protocoltranslator.viaproxy.ViaProxyConfig;
import net.raphimc.viaproxy.util.Proxy;

import java.net.URI;
import java.net.URISyntaxException;

public class ProxyTypeSerializer extends ConfigTypeSerializer<ViaProxyConfig, Proxy> {

    public ProxyTypeSerializer(final ViaProxyConfig config) {
        super(config);
    }

    @Override
    public Proxy deserialize(final Class<Proxy> typeClass, final Object serializedObject) {
        final String proxyUrl = (String) serializedObject;
        if (!proxyUrl.isBlank()) {
            try {
                return new Proxy(new URI(proxyUrl));
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Invalid proxy url: " + proxyUrl + ". Proxy url format: type://address:port or type://username:password@address:port");
            }
        }
        return null;
    }

    @Override
    public Object serialize(final Proxy object) {
        if (object != null) {
            return object.toURI().toString();
        } else {
            return "";
        }
    }

}
