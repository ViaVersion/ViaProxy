package net.raphimc.viaproxy.protocolhack.providers;

import com.viaversion.viaversion.api.connection.UserConnection;
import net.raphimc.vialegacy.protocols.release.protocol1_7_2_5to1_6_4.providers.EncryptionProvider;
import net.raphimc.viaproxy.proxy.ProxyConnection;

public class ViaProxyEncryptionProvider extends EncryptionProvider {

    @Override
    public void enableDecryption(UserConnection user) {
        try {
            ProxyConnection.fromUserConnection(user).enablePreNettyEncryption();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
