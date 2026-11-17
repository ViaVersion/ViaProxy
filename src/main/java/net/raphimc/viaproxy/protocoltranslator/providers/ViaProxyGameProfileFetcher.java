/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2026 RK_01/RaphiMC and contributors
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
package net.raphimc.viaproxy.protocoltranslator.providers;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ProfileNotFoundException;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.viaversion.viaversion.api.minecraft.GameProfile;
import net.raphimc.vialegacy.protocol.release.r1_7_6_10tor1_8.provider.GameProfileFetcher;
import net.raphimc.viaproxy.proxy.external_interface.AuthLibServices;

import java.util.Map;
import java.util.UUID;

public class ViaProxyGameProfileFetcher extends GameProfileFetcher {

    @Override
    public UUID loadMojangUuid(final String playerName) {
        return AuthLibServices.PROFILE_REPOSITORY.findProfileByName(playerName).orElseThrow(ProfileNotFoundException::new).id();
    }

    @Override
    public GameProfile loadGameProfile(final UUID uuid) {
        final ProfileResult result = AuthLibServices.SESSION_SERVICE.fetchProfile(uuid, true);
        if (result == null) {
            throw new ProfileNotFoundException();
        }

        final com.mojang.authlib.GameProfile gameProfile = result.profile();
        final GameProfile.Property[] properties = new GameProfile.Property[gameProfile.properties().size()];
        int i = 0;
        for (Map.Entry<String, Property> entry : gameProfile.properties().entries()) {
            properties[i++] = new GameProfile.Property(entry.getValue().name(), entry.getValue().value(), entry.getValue().signature());
        }
        return new GameProfile(gameProfile.name(), gameProfile.id(), properties);
    }

}
