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

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.VersionProvider;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.CompressionProvider;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.HandItemProvider;
import net.raphimc.vialegacy.protocols.classic.protocola1_0_15toc0_28_30.providers.ClassicCustomCommandProvider;
import net.raphimc.vialegacy.protocols.classic.protocola1_0_15toc0_28_30.providers.ClassicMPPassProvider;
import net.raphimc.vialegacy.protocols.classic.protocola1_0_15toc0_28_30.providers.ClassicWorldHeightProvider;
import net.raphimc.vialegacy.protocols.release.protocol1_3_1_2to1_2_4_5.providers.OldAuthProvider;
import net.raphimc.vialegacy.protocols.release.protocol1_7_2_5to1_6_4.providers.EncryptionProvider;
import net.raphimc.vialegacy.protocols.release.protocol1_8to1_7_6_10.providers.GameProfileFetcher;
import net.raphimc.viaprotocolhack.impl.viaversion.VPLoader;
import net.raphimc.viaproxy.plugins.PluginManager;
import net.raphimc.viaproxy.plugins.events.ViaLoadingEvent;
import net.raphimc.viaproxy.protocolhack.providers.*;

public class ViaProxyVPLoader extends VPLoader {

    @Override
    public void load() {
        super.load();

        Via.getManager().getProviders().use(CompressionProvider.class, new ViaProxyCompressionProvider());
        Via.getManager().getProviders().use(HandItemProvider.class, new ViaProxyHandItemProvider());
        Via.getManager().getProviders().use(VersionProvider.class, new ViaProxyVersionProvider());

        Via.getManager().getProviders().use(GameProfileFetcher.class, new ViaProxyGameProfileFetcher());
        Via.getManager().getProviders().use(EncryptionProvider.class, new ViaProxyEncryptionProvider());
        Via.getManager().getProviders().use(OldAuthProvider.class, new ViaProxyOldAuthProvider());
        Via.getManager().getProviders().use(ClassicWorldHeightProvider.class, new ViaProxyClassicWorldHeightProvider());
        Via.getManager().getProviders().use(ClassicCustomCommandProvider.class, new ViaProxyClassicCustomCommandProvider());
        Via.getManager().getProviders().use(ClassicMPPassProvider.class, new ViaProxyClassicMPPassProvider());

        PluginManager.EVENT_MANAGER.call(new ViaLoadingEvent());
    }

}
