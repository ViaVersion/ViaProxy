package net.raphimc.viaproxy.protocolhack.providers;

import com.viaversion.viaversion.api.connection.UserConnection;
import net.raphimc.vialegacy.protocols.release.protocol1_3_1_2to1_2_4_5.providers.OldAuthProvider;
import net.raphimc.viaproxy.proxy.ProxyConnection;
import net.raphimc.viaproxy.proxy.external_interface.ExternalInterface;

public class ViaProxyOldAuthProvider extends OldAuthProvider {

    @Override
    public void sendAuthRequest(final UserConnection user, final String serverId) throws Throwable {
        ExternalInterface.joinServer(serverId, ProxyConnection.fromUserConnection(user));
    }

}
