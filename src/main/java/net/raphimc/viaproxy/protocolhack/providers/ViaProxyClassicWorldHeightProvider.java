package net.raphimc.viaproxy.protocolhack.providers;

import com.viaversion.viaversion.api.connection.UserConnection;
import net.raphimc.vialegacy.protocols.classic.protocola1_0_15toc0_28_30.providers.ClassicWorldHeightProvider;
import net.raphimc.vialegacy.util.VersionEnum;
import net.raphimc.viaproxy.proxy.ProxyConnection;

public class ViaProxyClassicWorldHeightProvider extends ClassicWorldHeightProvider {

    @Override
    public short getMaxChunkSectionCount(UserConnection user) {
        if (ProxyConnection.fromUserConnection(user).getClientVersion().isNewerThanOrEqualTo(VersionEnum.r1_17)) {
            return 64;
        }
        return 16;
    }

}
