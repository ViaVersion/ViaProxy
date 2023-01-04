package net.raphimc.viaproxy.protocolhack.providers;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.protocols.base.BaseVersionProvider;
import net.raphimc.viaproxy.proxy.ProxyConnection;

public class ViaProxyVersionProvider extends BaseVersionProvider {

    @Override
    public int getClosestServerProtocol(UserConnection connection) throws Exception {
        if (connection.isClientSide()) return ProxyConnection.fromUserConnection(connection).getServerVersion().getVersion();
        return super.getClosestServerProtocol(connection);
    }

}
