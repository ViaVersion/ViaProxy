/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2025 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.viaproxy.injection.mixins;

import com.mojang.authlib.yggdrasil.ServicesKeyInfo;
import com.mojang.authlib.yggdrasil.YggdrasilServicesKeyInfo;
import net.lenni0451.reflect.stream.RStream;
import net.raphimc.netminecraft.netty.crypto.CryptUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.security.PublicKey;

@Mixin(YggdrasilServicesKeyInfo.class)
public abstract class MixinYggdrasilServicesKeyInfo {

    @Overwrite
    public static ServicesKeyInfo createFromResources() {
        try {
            return RStream.of(YggdrasilServicesKeyInfo.class).constructors().by(PublicKey.class).newInstance(CryptUtil.MOJANG_PUBLIC_KEY);
        } catch (Throwable e) {
            throw new AssertionError("Missing/invalid yggdrasil public key!", e);
        }
    }

}
