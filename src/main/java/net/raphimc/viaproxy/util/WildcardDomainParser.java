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

import java.net.SocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WildcardDomainParser {

    private static final Pattern PUBLIC_WILDCARD_FORMAT2_PATTERN = Pattern.compile("^address\\.(.+?)\\.port\\.(\\d+?)(?:\\.version\\.(.+?))?$");

    public static ParsedDomain parseFormat1(final String address) {
        try {
            final String addressData = address.substring(0, address.toLowerCase().lastIndexOf(".viaproxy."));
            final ArrayHelper arrayHelper = ArrayHelper.instanceOf(addressData.split(Pattern.quote("_")));
            if (arrayHelper.getLength() < 3) {
                return null;
            }
            final String versionString = arrayHelper.get(arrayHelper.getLength() - 1);
            ProtocolVersion serverVersion = ProtocolVersionUtil.fromNameLenient(versionString);
            final String connectAddress = arrayHelper.getAsString(0, arrayHelper.getLength() - 3, "_");
            final int connectPort = arrayHelper.getInteger(arrayHelper.getLength() - 2);
            return new ParsedDomain(AddressUtil.parse(connectAddress + ":" + connectPort, serverVersion), serverVersion);
        } catch (IllegalArgumentException | StringIndexOutOfBoundsException e) {
            return null;
        }
    }

    public static ParsedDomain parseFormat2(final String address) {
        try {
            final String addressData = address.substring(0, address.toLowerCase().lastIndexOf(".f2.viaproxy."));
            final Matcher matcher = PUBLIC_WILDCARD_FORMAT2_PATTERN.matcher(addressData);
            if (!matcher.matches()) {
                return null;
            }
            final String connectAddress = matcher.group(1);
            final int connectPort = Integer.parseInt(matcher.group(2));
            final String versionString = matcher.group(3);
            ProtocolVersion serverVersion;
            if (versionString != null) { // Version part is optional
                serverVersion = ProtocolVersionUtil.fromNameLenient(versionString);
            } else { // Default to auto-detect
                serverVersion = ProtocolTranslator.AUTO_DETECT_PROTOCOL;
            }
            return new ParsedDomain(AddressUtil.parse(connectAddress + ":" + connectPort, serverVersion), serverVersion);
        } catch (IllegalArgumentException | StringIndexOutOfBoundsException e) {
            return null;
        }
    }


    public record ParsedDomain(SocketAddress address, ProtocolVersion version) {
    }

}
