package net.raphimc.viaproxy.protocolhack.impl;

import com.viaversion.viaversion.api.Via;
import de.gerrygames.viarewind.api.ViaRewindConfigImpl;
import de.gerrygames.viarewind.api.ViaRewindPlatform;
import net.raphimc.viaprotocolhack.util.JLoggerToSLF4J;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.logging.Logger;

public class ViaProxyViaRewindPlatformImpl implements ViaRewindPlatform {

    private static final Logger LOGGER = new JLoggerToSLF4J(LoggerFactory.getLogger("ViaRewind"));

    public ViaProxyViaRewindPlatformImpl() {
        new ViaRewindConfigImpl(new File(Via.getPlatform().getDataFolder(), "viarewind.yml")) {
            @Override
            public URL getDefaultConfigURL() {
                return ViaProxyViaVersionPlatformImpl.class.getClassLoader().getResource("assets/viaproxy/config_diff/viarewind.yml");
            }
        }.reloadConfig();

        final ViaRewindConfigImpl config = new ViaRewindConfigImpl(new File(Via.getPlatform().getDataFolder(), "viarewind.yml"));
        config.reloadConfig();
        this.init(config);
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

}
