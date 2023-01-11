package net.raphimc.viaproxy.injection.transformer;

import com.mojang.authlib.yggdrasil.ServicesKeyInfo;
import com.mojang.authlib.yggdrasil.YggdrasilServicesKeyInfo;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.COverride;
import net.lenni0451.reflect.stream.RStream;
import net.raphimc.netminecraft.netty.crypto.CryptUtil;

import java.security.PublicKey;

@CTransformer(YggdrasilServicesKeyInfo.class)
public abstract class YggdrasilServicesKeyInfoTransformer {

    @COverride
    public static ServicesKeyInfo createFromResources() {
        try {
            return RStream.of(YggdrasilServicesKeyInfo.class).constructors().by(PublicKey.class).newInstance(CryptUtil.MOJANG_PUBLIC_KEY);
        } catch (Throwable e) {
            throw new AssertionError("Missing/invalid yggdrasil public key!", e);
        }
    }

}
