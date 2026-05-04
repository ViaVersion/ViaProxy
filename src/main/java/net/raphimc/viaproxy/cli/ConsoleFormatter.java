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
package net.raphimc.viaproxy.cli;

import net.lenni0451.mcstructs.text.TextComponent;
import net.lenni0451.mcstructs.text.stringformat.StringFormat;
import net.lenni0451.mcstructs.text.stringformat.handling.ColorHandling;
import net.lenni0451.mcstructs.text.stringformat.handling.SerializerUnknownHandling;

public class ConsoleFormatter {

    private static final StringFormat VANILLA_FORMAT = StringFormat.vanilla();
    private static final StringFormat ANSI_FORMAT = StringFormat.ansi();

    public static String convert(final String legacyString) {
        return VANILLA_FORMAT.convertTo(legacyString, ANSI_FORMAT);
    }

    public static String convert(final TextComponent component) {
        return ANSI_FORMAT.toString(component, ColorHandling.RESET, SerializerUnknownHandling.IGNORE);
    }

}
