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
package net.raphimc.viaproxy.util.config;

import net.lenni0451.optconfig.serializer.ConfigTypeSerializer;
import net.lenni0451.optconfig.serializer.info.DeserializerInfo;
import net.lenni0451.optconfig.serializer.info.SerializerInfo;
import net.raphimc.viaproxy.util.AddressUtil;

import java.net.SocketAddress;

public class SocketAddressTypeSerializer implements ConfigTypeSerializer<SocketAddress> {

    @Override
    public SocketAddress deserialize(final DeserializerInfo<SocketAddress> info) {
        return AddressUtil.parse((String) info.value(), null);
    }

    @Override
    public Object serialize(final SerializerInfo<SocketAddress> info) {
        return AddressUtil.toString(info.value());
    }

}
