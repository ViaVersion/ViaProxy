package net.raphimc.viaproxy.protocolhack;

import net.raphimc.viaprotocolhack.ViaProtocolHack;
import net.raphimc.viaprotocolhack.impl.platform.ViaLegacyPlatformImpl;
import net.raphimc.viaproxy.protocolhack.impl.*;

public class ProtocolHack {

    public static void init() {
        ViaProtocolHack.init(new ViaProxyViaVersionPlatformImpl(), new ViaProxyVPLoader(), null, null, ViaProxyViaBackwardsPlatformImpl::new, ViaProxyViaRewindPlatformImpl::new, ViaLegacyPlatformImpl::new);
    }

}
