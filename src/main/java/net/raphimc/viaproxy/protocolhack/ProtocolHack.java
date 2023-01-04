package net.raphimc.viaproxy.protocolhack;

import net.raphimc.viaprotocolhack.ViaProtocolHack;
import net.raphimc.viaprotocolhack.impl.platform.*;
import net.raphimc.viaproxy.protocolhack.impl.ViaProxyVPLoader;
import net.raphimc.viaproxy.protocolhack.impl.ViaProxyViaVersionPlatformImpl;

public class ProtocolHack {

    public static void init() {
        ViaProtocolHack.init(new ViaProxyViaVersionPlatformImpl(), new ViaProxyVPLoader(), null, null, ViaBackwardsPlatformImpl::new, ViaRewindPlatformImpl::new, ViaLegacyPlatformImpl::new);
    }

}
