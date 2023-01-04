package net.raphimc.viaproxy.protocolhack.providers;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.HandItemProvider;

public class ViaProxyHandItemProvider extends HandItemProvider {

    @Override
    public Item getHandItem(final UserConnection info) {
        return null;
    }

}
