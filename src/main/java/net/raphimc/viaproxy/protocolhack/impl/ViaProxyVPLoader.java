package net.raphimc.viaproxy.protocolhack.impl;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.VersionProvider;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.CompressionProvider;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.HandItemProvider;
import net.raphimc.vialegacy.protocols.classic.protocola1_0_15toc0_28_30.providers.*;
import net.raphimc.vialegacy.protocols.release.protocol1_3_1_2to1_2_4_5.providers.OldAuthProvider;
import net.raphimc.vialegacy.protocols.release.protocol1_7_2_5to1_6_4.providers.EncryptionProvider;
import net.raphimc.vialegacy.protocols.release.protocol1_8to1_7_6_10.providers.GameProfileFetcher;
import net.raphimc.viaprotocolhack.impl.viaversion.VPLoader;
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
    }

}
