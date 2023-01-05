package net.raphimc.viaproxy.protocolhack.impl;

import com.viaversion.viaversion.configuration.AbstractViaConfig;
import net.raphimc.viaprotocolhack.impl.platform.ViaVersionPlatformImpl;
import net.raphimc.viaprotocolhack.impl.viaversion.VPViaConfig;
import net.raphimc.viaproxy.cli.ConsoleFormatter;

import java.io.File;
import java.net.URL;
import java.util.UUID;

public class ViaProxyViaVersionPlatformImpl extends ViaVersionPlatformImpl {

    public ViaProxyViaVersionPlatformImpl() {
        super(null);
    }

    @Override
    public void sendMessage(UUID uuid, String msg) {
        super.sendMessage(uuid, ConsoleFormatter.convert(msg));
    }

    @Override
    protected AbstractViaConfig createConfig() {
        new VPViaConfig(new File(this.getDataFolder(), "viaversion.yml")) {
            @Override
            public URL getDefaultConfigURL() {
                return ViaProxyViaVersionPlatformImpl.class.getClassLoader().getResource("assets/viaproxy/config_diff/viaversion.yml");
            }
        }.reloadConfig();

        return super.createConfig();
    }

}
