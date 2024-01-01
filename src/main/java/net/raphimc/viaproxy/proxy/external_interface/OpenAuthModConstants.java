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
package net.raphimc.viaproxy.proxy.external_interface;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class OpenAuthModConstants {

    public static final String BASE_CHANNEL = "oam:";
    public static final byte[] LEGACY_MAGIC_BYTES = new byte[]{2, 20, 12, 3}; // 1.8 - 1.12.2
    public static final String LEGACY_MAGIC_STRING = new String(LEGACY_MAGIC_BYTES, StandardCharsets.UTF_8); // 1.8 - 1.12.2
    public static final int LEGACY_MAGIC_INT = -new BigInteger(LEGACY_MAGIC_BYTES).intValueExact(); // 1.8 - 1.12.2

    // Request
    public static final String JOIN_CHANNEL = BASE_CHANNEL + "join"; // 1.8 - latest
    public static final String SIGN_NONCE_CHANNEL = BASE_CHANNEL + "sign_nonce"; // 1.19 - latest

    // Response
    public static final String DATA_CHANNEL = BASE_CHANNEL + "data"; // 1.8 - latest

}
