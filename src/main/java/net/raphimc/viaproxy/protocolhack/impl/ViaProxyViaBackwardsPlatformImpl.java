package net.raphimc.viaproxy.protocolhack.impl;

import com.viaversion.viabackwards.ViaBackwardsConfig;
import net.raphimc.viaprotocolhack.impl.platform.ViaBackwardsPlatformImpl;

import java.io.File;
import java.net.URL;

public class ViaProxyViaBackwardsPlatformImpl extends ViaBackwardsPlatformImpl {

    @Override
    public void init(File dataFolder) {
        new ViaBackwardsConfig(new File(dataFolder, "config.yml")) {
            @Override
            public URL getDefaultConfigURL() {
                return ViaProxyViaVersionPlatformImpl.class.getClassLoader().getResource("assets/viaproxy/config_diff/viabackwards.yml");
            }
        }.reloadConfig();

        super.init(dataFolder);
    }

}
