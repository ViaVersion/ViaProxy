package net.raphimc.viaproxy.protocolhack.impl;

import net.raphimc.viaprotocolhack.impl.platform.ViaVersionPlatformImpl;
import net.raphimc.viaproxy.cli.ConsoleFormatter;

import java.util.UUID;

public class ViaProxyViaVersionPlatformImpl extends ViaVersionPlatformImpl {

    public ViaProxyViaVersionPlatformImpl() {
        super(null);
    }

    @Override
    public void sendMessage(UUID uuid, String msg) {
        super.sendMessage(uuid, ConsoleFormatter.convert(msg));
    }

}
