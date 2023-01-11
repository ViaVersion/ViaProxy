package net.raphimc.viaproxy.proxy.external_interface;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

import java.net.Proxy;
import java.util.UUID;

public class AuthLibServices {

    public static final HttpAuthenticationService authenticationService = new YggdrasilAuthenticationService(Proxy.NO_PROXY, UUID.randomUUID().toString());
    public static final MinecraftSessionService sessionService = authenticationService.createMinecraftSessionService();
    public static final GameProfileRepository gameProfileRepository = authenticationService.createProfileRepository();

}
