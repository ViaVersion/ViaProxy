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
package net.raphimc.viaproxy.protocoltranslator;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionType;
import net.raphimc.vialoader.ViaLoader;
import net.raphimc.vialoader.impl.platform.ViaAprilFoolsPlatformImpl;
import net.raphimc.vialoader.impl.platform.ViaBackwardsPlatformImpl;
import net.raphimc.vialoader.impl.platform.ViaBedrockPlatformImpl;
import net.raphimc.vialoader.impl.platform.ViaRewindPlatformImpl;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.plugins.events.ProtocolTranslatorInitEvent;
import net.raphimc.viaproxy.protocoltranslator.impl.ViaProxyVLLoader;
import net.raphimc.viaproxy.protocoltranslator.impl.ViaProxyViaLegacyPlatformImpl;
import net.raphimc.viaproxy.protocoltranslator.impl.ViaProxyViaVersionPlatformImpl;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ProtocolTranslator {

    public static final ProtocolVersion AUTO_DETECT_PROTOCOL = new ProtocolVersion(VersionType.SPECIAL, -2, -1, "Auto Detect (1.7+ servers)", null) {
        @Override
        protected Comparator<ProtocolVersion> customComparator() {
            return (o1, o2) -> {
                if (o1 == AUTO_DETECT_PROTOCOL) {
                    return -1;
                } else if (o2 == AUTO_DETECT_PROTOCOL) {
                    return 1;
                } else {
                    return 0;
                }
            };
        }

        @Override
        public boolean isKnown() {
            return false;
        }
    };

    public static void init() {
        patchConfigs();
        final Supplier<?>[] platformSuppliers = ViaProxy.EVENT_MANAGER.call(new ProtocolTranslatorInitEvent(ViaBackwardsPlatformImpl::new, ViaRewindPlatformImpl::new, ViaProxyViaLegacyPlatformImpl::new, ViaAprilFoolsPlatformImpl::new, ViaBedrockPlatformImpl::new)).getPlatformSuppliers().toArray(new Supplier[0]);
        ViaLoader.init(new ViaProxyViaVersionPlatformImpl(), new ViaProxyVLLoader(), null, null, platformSuppliers);
        ProtocolVersion.register(AUTO_DETECT_PROTOCOL);
    }

    private static void patchConfigs() {
        final File configFolder = new File("ViaLoader");

        final File viaVersionConfig = new File(configFolder, "viaversion.yml");
        final Map<String, Object> viaVersionPatches = new HashMap<>();
        viaVersionPatches.put("1_13-tab-complete-delay", 5);
        viaVersionPatches.put("no-delay-shield-blocking", true);
        viaVersionPatches.put("chunk-border-fix", true);
        new ConfigPatcher(viaVersionConfig, viaVersionPatches);

        final File viaBackwardsConfig = new File(configFolder, "viabackwards.yml");
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
