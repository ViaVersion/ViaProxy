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
package net.raphimc.viaproxy.protocolhack;

import net.raphimc.viaprotocolhack.ViaProtocolHack;
import net.raphimc.viaprotocolhack.impl.platform.ViaBackwardsPlatformImpl;
import net.raphimc.viaprotocolhack.impl.platform.ViaLegacyPlatformImpl;
import net.raphimc.viaprotocolhack.impl.platform.ViaRewindPlatformImpl;
import net.raphimc.viaproxy.plugins.PluginManager;
import net.raphimc.viaproxy.plugins.events.ProtocolHackInitEvent;
import net.raphimc.viaproxy.protocolhack.impl.ViaAprilFoolsPlatformImpl;
import net.raphimc.viaproxy.protocolhack.impl.ViaProxyVPLoader;
import net.raphimc.viaproxy.protocolhack.impl.ViaProxyViaVersionPlatformImpl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ProtocolHack {

    public static void init() {
        patchConfigs();
        final Supplier<?>[] platformSuppliers = PluginManager.EVENT_MANAGER.call(new ProtocolHackInitEvent(ViaBackwardsPlatformImpl::new, ViaRewindPlatformImpl::new, ViaLegacyPlatformImpl::new, ViaAprilFoolsPlatformImpl::new)).getPlatformSuppliers().toArray(new Supplier[0]);
        ViaProtocolHack.init(new ViaProxyViaVersionPlatformImpl(), new ViaProxyVPLoader(), null, null, platformSuppliers);
    }

    private static void patchConfigs() {
        final File configFolder = new File("ViaProtocolHack");

        final File viaVersionConfig = new File(configFolder, "viaversion.yml");
        final Map<String, Object> viaVersionPatches = new HashMap<>();
        viaVersionPatches.put("1_13-tab-complete-delay", 5);
        viaVersionPatches.put("no-delay-shield-blocking", true);
        viaVersionPatches.put("chunk-border-fix", true);
        new ConfigPatcher(viaVersionConfig, viaVersionPatches);

        final File viaBackwardsConfig = new File(configFolder, "config.yml");
        final Map<String, Object> viaBackwardsPatches = new HashMap<>();
        viaBackwardsPatches.put("fix-1_13-face-player", true);
        viaBackwardsPatches.put("handle-pings-as-inv-acknowledgements", true);
        new ConfigPatcher(viaBackwardsConfig, viaBackwardsPatches);

        final File viaRewindConfig = new File(configFolder, "viarewind.yml");
        final Map<String, Object> viaRewindPatches = new HashMap<>();
        viaRewindPatches.put("replace-adventure", true);
        viaRewindPatches.put("replace-particles", true);
        new ConfigPatcher(viaRewindConfig, viaRewindPatches);
    }

}
