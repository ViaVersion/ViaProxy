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
package net.raphimc.viaproxy.injection.transformer;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.COverride;
import net.raphimc.viaprotocolhack.util.VersionEnum;
import net.raphimc.viaproxy.plugins.PluginManager;
import net.raphimc.viaproxy.plugins.ViaProxyPlugin;

@CTransformer(VersionEnum.class)
public abstract class VersionEnumTransformer {

    @COverride
    private static ProtocolVersion getViaBedrockProtocol(final String name) {
        try {
            Class<?> clazz = null;
            for (ViaProxyPlugin plugin : PluginManager.getPlugins()) {
                try {
                    clazz = Class.forName("net.raphimc.viabedrock.api.BedrockProtocolVersion", true, plugin.getClass().getClassLoader());
                    break;
                } catch (Throwable ignored) {
                }
            }
            if (clazz != null) {
                return (ProtocolVersion) clazz.getField(name).get(null);
            }
        } catch (Throwable ignored) {
        }
        return ProtocolVersion.unknown;
    }

}
