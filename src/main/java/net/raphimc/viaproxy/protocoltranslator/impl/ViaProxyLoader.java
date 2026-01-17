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
package net.raphimc.viaproxy.protocoltranslator.impl;

import com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.provider.TransferProvider;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.platform.ViaPlatformLoader;
import com.viaversion.viaversion.api.protocol.version.VersionProvider;
import com.viaversion.viaversion.protocols.v1_8to1_9.provider.CompressionProvider;
import net.raphimc.viabedrock.protocol.provider.NettyPipelineProvider;
import net.raphimc.vialegacy.protocol.classic.c0_28_30toa1_0_15.provider.ClassicCustomCommandProvider;
import net.raphimc.vialegacy.protocol.classic.c0_28_30toa1_0_15.provider.ClassicMPPassProvider;
import net.raphimc.vialegacy.protocol.classic.c0_28_30toa1_0_15.provider.ClassicWorldHeightProvider;
import net.raphimc.vialegacy.protocol.release.r1_2_4_5tor1_3_1_2.provider.OldAuthProvider;
import net.raphimc.vialegacy.protocol.release.r1_6_4tor1_7_2_5.provider.EncryptionProvider;
import net.raphimc.vialegacy.protocol.release.r1_7_6_10tor1_8.provider.GameProfileFetcher;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.plugins.events.ViaLoadingEvent;
import net.raphimc.viaproxy.protocoltranslator.providers.*;

public class ViaProxyLoader implements ViaPlatformLoader {

    @Override
    public void load() {
        // ViaVersion
        Via.getManager().getProviders().use(CompressionProvider.class, new ViaProxyCompressionProvider());
        Via.getManager().getProviders().use(VersionProvider.class, new ViaProxyVersionProvider());

        // ViaBackwards
        Via.getManager().getProviders().use(TransferProvider.class, new ViaProxyTransferProvider());

        // ViaLegacy
        Via.getManager().getProviders().use(GameProfileFetcher.class, new ViaProxyGameProfileFetcher());
        Via.getManager().getProviders().use(EncryptionProvider.class, new ViaProxyEncryptionProvider());
        Via.getManager().getProviders().use(OldAuthProvider.class, new ViaProxyOldAuthProvider());
        Via.getManager().getProviders().use(ClassicWorldHeightProvider.class, new ViaProxyClassicWorldHeightProvider());
        Via.getManager().getProviders().use(ClassicCustomCommandProvider.class, new ViaProxyClassicCustomCommandProvider());
        Via.getManager().getProviders().use(ClassicMPPassProvider.class, new ViaProxyClassicMPPassProvider());

        // ViaBedrock
        Via.getManager().getProviders().use(NettyPipelineProvider.class, new ViaProxyNettyPipelineProvider());

        // ViaProxy plugins
        ViaProxy.EVENT_MANAGER.call(new ViaLoadingEvent());
    }

    @Override
    public void unload() {
    }

}
