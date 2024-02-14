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
package net.raphimc.viaproxy.cli.options;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import joptsimple.ValueConversionException;
import joptsimple.ValueConverter;

public class ProtocolVersionConverter implements ValueConverter<ProtocolVersion> {

    @Override
    public ProtocolVersion convert(String s) {
        final ProtocolVersion version = ProtocolVersion.getClosest(s);
        if (version == null) {
            throw new ValueConversionException("Unable to find version '" + s + "'");
        }

        return version;
    }

    @Override
    public Class<ProtocolVersion> valueType() {
        return ProtocolVersion.class;
    }

    @Override
    public String valuePattern() {
        StringBuilder s = new StringBuilder();
        for (ProtocolVersion version : ProtocolVersion.getProtocols()) {
            s.append((s.isEmpty()) ? "" : ", ").append(version.getName());
        }
        return "[" + s + "]";
    }

}
