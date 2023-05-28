/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
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

import joptsimple.ValueConversionException;
import joptsimple.ValueConverter;
import net.raphimc.vialoader.util.VersionEnum;

public class VersionEnumConverter implements ValueConverter<VersionEnum> {

    @Override
    public VersionEnum convert(String s) {
        for (VersionEnum version : VersionEnum.getAllVersions()) {
            if (version.getName().equalsIgnoreCase(s)) return version;
        }
        throw new ValueConversionException("Unable to find version '" + s + "'");
    }

    @Override
    public Class<VersionEnum> valueType() {
        return VersionEnum.class;
    }

    @Override
    public String valuePattern() {
        StringBuilder s = new StringBuilder();
        for (VersionEnum version : VersionEnum.getAllVersions()) {
            s.append((s.length() == 0) ? "" : ", ").append(version.getName());
        }
        return "[" + s + "]";
    }

}
