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

import com.viaversion.viaversion.configuration.AbstractViaConfig;
import net.raphimc.viaprotocolhack.impl.platform.ViaVersionPlatformImpl;
import net.raphimc.viaprotocolhack.impl.viaversion.VPViaConfig;
import net.raphimc.viaproxy.cli.ConsoleFormatter;

import java.io.File;
import java.net.URL;
import java.util.UUID;

public class ViaProxyViaVersionPlatformImpl extends ViaVersionPlatformImpl {

    public ViaProxyViaVersionPlatformImpl() {
        super(null);
    }

    @Override
    public void sendMessage(UUID uuid, String msg) {
        super.sendMessage(uuid, ConsoleFormatter.convert(msg));
    }

    @Override
    protected AbstractViaConfig createConfig() {
        new VPViaConfig(new File(this.getDataFolder(), "viaversion.yml")) {
            @Override
            public URL getDefaultConfigURL() {
                return ViaProxyViaVersionPlatformImpl.class.getClassLoader().getResource("assets/viaproxy/config_diff/viaversion.yml");
            }
        }.reloadConfig();

        return super.createConfig();
    }

}
