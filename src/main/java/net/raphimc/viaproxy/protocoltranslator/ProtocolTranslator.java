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
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.Protocol1_20_5To1_20_3;
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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
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
        patchConfigs();
        final Supplier<?>[] platformSuppliers = ViaProxy.EVENT_MANAGER.call(new ProtocolTranslatorInitEvent(ViaBackwardsPlatformImpl::new, ViaRewindPlatformImpl::new, ViaProxyViaLegacyPlatformImpl::new, ViaAprilFoolsPlatformImpl::new, ViaBedrockPlatformImpl::new)).getPlatformSuppliers().toArray(new Supplier[0]);
        ViaLoader.init(new ViaProxyViaVersionPlatformImpl(), new ViaProxyVLLoader(), null, null, platformSuppliers);
        Protocol1_20_5To1_20_3.strictErrorHandling = false;
        ProtocolVersion.register(AUTO_DETECT_PROTOCOL);
    }

    private static void patchConfigs() {
        final File configFolder = new File("ViaLoader");
        configFolder.mkdirs();

        try {
            final File viaVersionConfig = new File(configFolder, "viaversion.yml");
            Files.writeString(viaVersionConfig.toPath(), """
                    1_13-tab-complete-delay: 5
                    no-delay-shield-blocking: true
                    chunk-border-fix: true
                    """, StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException ignored) {
        } catch (Throwable e) {
            throw new RuntimeException("Failed to patch ViaVersion config", e);
        }

        try {
            final File viaBackwardsConfig = new File(configFolder, "viabackwards.yml");
            Files.writeString(viaBackwardsConfig.toPath(), """
                    fix-1_13-face-player: 5
                    handle-pings-as-inv-acknowledgements: true
                    """, StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException ignored) {
        } catch (Throwable e) {
            throw new RuntimeException("Failed to patch ViaBackwards config", e);
        }

        try {
            final File viaRewindConfig = new File(configFolder, "viarewind.yml");
            Files.writeString(viaRewindConfig.toPath(), """
                    replace-adventure: true
                    replace-particles: true
                    """, StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException ignored) {
        } catch (Throwable e) {
            throw new RuntimeException("Failed to patch ViaRewind config", e);
        }
    }

}
