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
package net.raphimc.viaproxy.protocoltranslator.impl;

import net.raphimc.vialoader.impl.platform.ViaLegacyPlatformImpl;
import net.raphimc.viaproxy.ViaProxy;

public class ViaProxyViaLegacyPlatformImpl extends ViaLegacyPlatformImpl {

    @Override
    public String getCpeAppName() {
        return "ViaProxy " + ViaProxy.VERSION;
    }

}
