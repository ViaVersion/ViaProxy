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
package net.raphimc.viaproxy.protocoltranslator;

import com.viaversion.viaaprilfools.ViaAprilFoolsPlatformImpl;
import com.viaversion.viabackwards.ViaBackwardsPlatformImpl;
import com.viaversion.viarewind.ViaRewindPlatformImpl;
import com.viaversion.viaversion.ViaManagerImpl;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionType;
import com.viaversion.viaversion.commands.ViaCommandHandler;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.Protocol1_20_3To1_20_5;
import net.lenni0451.classtransform.utils.log.Logger;
import net.raphimc.viabedrock.ViaBedrockPlatformImpl;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.plugins.events.ProtocolTranslatorInitEvent;
import net.raphimc.viaproxy.protocoltranslator.impl.ViaProxyInjector;
import net.raphimc.viaproxy.protocoltranslator.impl.ViaProxyPlatformLoader;
import net.raphimc.viaproxy.protocoltranslator.impl.ViaProxyViaLegacyPlatform;
import net.raphimc.viaproxy.protocoltranslator.impl.ViaProxyViaVersionPlatform;
import net.raphimc.viaproxy.util.YamlConfigPatcher;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ProtocolTranslator {

    public static final ProtocolVersion AUTO_DETECT_PROTOCOL = new ProtocolVersion(VersionType.SPECIAL, -2, -1, "Auto Detect (1.7+ servers)", null) {
        @Override
        protected Comparator<ProtocolVersion> customComparator() {
            return (o1, o2) -> {
                if (o1 == AUTO_DETECT_PROTOCOL) {
                    return 1;
                } else if (o2 == AUTO_DETECT_PROTOCOL) {
                    return -1;
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
        moveConfigs();
        patchConfigs();
        final Supplier<?>[] platformSuppliers = ViaProxy.EVENT_MANAGER.call(new ProtocolTranslatorInitEvent(ViaBackwardsPlatformImpl::new, ViaRewindPlatformImpl::new, ViaProxyViaLegacyPlatform::new, ViaAprilFoolsPlatformImpl::new, ViaBedrockPlatformImpl::new)).getPlatformSuppliers().toArray(new Supplier[0]);
        ViaManagerImpl.initAndLoad(new ViaProxyViaVersionPlatform(), new ViaProxyInjector(), new ViaCommandHandler(false), new ViaProxyPlatformLoader(), () -> {
            for (Supplier<?> platformSupplier : platformSuppliers) {
                platformSupplier.get();
            }
        });
        Protocol1_20_3To1_20_5.strictErrorHandling = false;
        ProtocolVersion.register(AUTO_DETECT_PROTOCOL);
    }

    // Migrate from old config location (ViaLoader folder) to new one
    private static void moveConfigs() {
        try {
            final File oldConfigDir = new File(ViaProxy.getCwd(), "ViaLoader");
            if (!oldConfigDir.exists() || !oldConfigDir.isDirectory()) {
                return;
            }
            for (File file : oldConfigDir.listFiles()) {
                FileUtils.moveToDirectory(file, ViaProxy.getCwd(), true);
            }
            FileUtils.deleteDirectory(oldConfigDir);
        } catch (Throwable e) {
            Logger.error("Failed to migrate old ViaLoader configs", e);
        }
    }

    private static void patchConfigs() {
        try {
            final YamlConfigPatcher configPatcher = new YamlConfigPatcher(new File(ViaProxy.getCwd(), "viaversion.yml"));
            final Map<String, Object> config = configPatcher.getConfig();
            config.putIfAbsent("1_13-tab-complete-delay", 5);
            config.putIfAbsent("no-delay-shield-blocking", true);
            config.putIfAbsent("handle-invalid-item-count", true);
            config.putIfAbsent("send-player-details", false);
            final Map<String, Object> packetLimiter = (Map<String, Object>) config.computeIfAbsent("packet-limiter", k -> new LinkedHashMap<>());
            packetLimiter.putIfAbsent("max-per-second", 1400);
            packetLimiter.putIfAbsent("sustained-max-per-second", 400);
            final Map<String, Object> logging = (Map<String, Object>) config.computeIfAbsent("logging", k -> new LinkedHashMap<>());
            logging.putIfAbsent("log-text-component-conversion-errors", true);
            logging.putIfAbsent("log-other-conversion-warnings", true);
            configPatcher.write();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to patch ViaVersion config", e);
        }

        try {
            final YamlConfigPatcher configPatcher = new YamlConfigPatcher(new File(ViaProxy.getCwd(), "viabackwards.yml"));
            final Map<String, Object> config = configPatcher.getConfig();
            config.putIfAbsent("fix-1_13-face-player", true);
            config.putIfAbsent("handle-pings-as-inv-acknowledgements", true);
            configPatcher.write();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to patch ViaBackwards config", e);
        }

        try {
            final YamlConfigPatcher configPatcher = new YamlConfigPatcher(new File(ViaProxy.getCwd(), "viarewind.yml"));
            final Map<String, Object> config = configPatcher.getConfig();
            config.putIfAbsent("replace-adventure", true);
            config.putIfAbsent("replace-particles", true);
            configPatcher.write();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to patch ViaRewind config", e);
        }
    }

}
