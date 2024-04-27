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
package net.raphimc.viaproxy.cli;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import joptsimple.BuiltinHelpFormatter;
import joptsimple.OptionDescriptor;
import joptsimple.internal.Classes;
import joptsimple.internal.Strings;

import java.util.List;

public class BetterHelpFormatter extends BuiltinHelpFormatter {

    public BetterHelpFormatter() {
        super(120, 2);
    }

    @Override
    protected String extractTypeIndicator(OptionDescriptor descriptor) {
        String indicator = descriptor.argumentTypeIndicator();
        if (indicator != null) {
            indicator = indicator.substring(indicator.indexOf('$') + 1);
            if (indicator.startsWith("[")) {
                return indicator.substring(1, indicator.length() - 1);
            }
        }
        return !Strings.isNullOrEmpty(indicator) && !String.class.getName().equals(indicator) ? Classes.shortNameOf(indicator) : "String";
    }

    @Override
    protected String createDefaultValuesDisplay(List<?> defaultValues) {
        if (defaultValues.size() == 1) {
            Object value = defaultValues.get(0);
            if (value instanceof ProtocolVersion version) {
                return version.getName();
            } else if (value instanceof String) {
                return "\"" + value + "\"";
            }

            return value.toString();
        }

        return defaultValues.toString();
    }

}
