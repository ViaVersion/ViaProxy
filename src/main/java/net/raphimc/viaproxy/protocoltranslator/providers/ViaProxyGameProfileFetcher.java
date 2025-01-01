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
package net.raphimc.viaproxy.protocoltranslator.providers;

import com.mojang.authlib.Agent;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ProfileNotFoundException;
import net.raphimc.vialegacy.protocol.release.r1_7_6_10tor1_8.model.GameProfile;
import net.raphimc.vialegacy.protocol.release.r1_7_6_10tor1_8.provider.GameProfileFetcher;
import net.raphimc.viaproxy.proxy.external_interface.AuthLibServices;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ViaProxyGameProfileFetcher extends GameProfileFetcher {

    @Override
    public UUID loadMojangUUID(String playerName) throws ExecutionException, InterruptedException {
        final CompletableFuture<com.mojang.authlib.GameProfile> future = new CompletableFuture<>();
        AuthLibServices.PROFILE_REPOSITORY.findProfilesByNames(new String[]{playerName}, Agent.MINECRAFT, new ProfileLookupCallback() {
            @Override
            public void onProfileLookupSucceeded(com.mojang.authlib.GameProfile gameProfile) {
                future.complete(gameProfile);
            }

            @Override
            public void onProfileLookupFailed(com.mojang.authlib.GameProfile gameProfile, Exception e) {
                future.completeExceptionally(e);
            }
        });
        if (!future.isDone()) {
            future.completeExceptionally(new ProfileNotFoundException());
        }
        return future.get().getId();
    }

    @Override
    public GameProfile loadGameProfile(UUID uuid) {
        final com.mojang.authlib.GameProfile inProfile = new com.mojang.authlib.GameProfile(uuid, null);
        final com.mojang.authlib.GameProfile mojangProfile = AuthLibServices.SESSION_SERVICE.fillProfileProperties(inProfile, true);
        if (mojangProfile.equals(inProfile)) throw new ProfileNotFoundException();

        final GameProfile gameProfile = new GameProfile(mojangProfile.getName(), mojangProfile.getId());
        for (Map.Entry<String, Property> entry : mojangProfile.getProperties().entries()) {
            final Property prop = entry.getValue();
            gameProfile.addProperty(new GameProfile.Property(prop.getName(), prop.getValue(), prop.getSignature()));
        }
        return gameProfile;
    }

}
