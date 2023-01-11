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
package net.raphimc.viaproxy.protocolhack.impl;

import com.viaversion.viabackwards.ViaBackwardsConfig;
import net.raphimc.viaprotocolhack.impl.platform.ViaBackwardsPlatformImpl;

import java.io.File;
import java.net.URL;

public class ViaProxyViaBackwardsPlatformImpl extends ViaBackwardsPlatformImpl {

    @Override
    public void init(File dataFolder) {
        new ViaBackwardsConfig(new File(dataFolder, "config.yml")) {
            @Override
            public URL getDefaultConfigURL() {
                return ViaProxyViaVersionPlatformImpl.class.getClassLoader().getResource("assets/viaproxy/config_diff/viabackwards.yml");
            }
        }.reloadConfig();

        super.init(dataFolder);
    }

}
