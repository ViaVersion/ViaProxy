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
package net.raphimc.viaproxy.util;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.raphimc.viaproxy.protocoltranslator.ProtocolTranslator;

public class ProtocolVersionUtil {

    public static ProtocolVersion fromNameLenient(final String name) {
        if (name.equalsIgnoreCase("auto")) { // Short form for auto-detect
            return ProtocolTranslator.AUTO_DETECT_PROTOCOL;
        }

        final ProtocolVersion version = ProtocolVersion.getClosest(name);
        if (version == null) {
            return ProtocolVersion.getClosest(name.replace("-", " "));
        }
        return version;
    }

}
