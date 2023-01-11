package net.raphimc.viaproxy.protocolhack.providers;

import com.mojang.authlib.Agent;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ProfileNotFoundException;
import net.raphimc.vialegacy.protocols.release.protocol1_8to1_7_6_10.model.GameProfile;
import net.raphimc.vialegacy.protocols.release.protocol1_8to1_7_6_10.providers.GameProfileFetcher;
import net.raphimc.viaproxy.proxy.external_interface.AuthLibServices;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ViaProxyGameProfileFetcher extends GameProfileFetcher {

    @Override
    public UUID loadMojangUUID(String playerName) throws ExecutionException, InterruptedException {
        final CompletableFuture<com.mojang.authlib.GameProfile> future = new CompletableFuture<>();
        AuthLibServices.gameProfileRepository.findProfilesByNames(new String[]{playerName}, Agent.MINECRAFT, new ProfileLookupCallback() {
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
        final com.mojang.authlib.GameProfile mojangProfile = AuthLibServices.sessionService.fillProfileProperties(inProfile, true);
        if (mojangProfile.equals(inProfile)) throw new ProfileNotFoundException();

        final GameProfile gameProfile = new GameProfile(mojangProfile.getName(), mojangProfile.getId());
        for (Map.Entry<String, Property> entry : mojangProfile.getProperties().entries()) {
            final Property prop = entry.getValue();
            gameProfile.addProperty(new GameProfile.Property(prop.getName(), prop.getValue(), prop.getSignature()));
        }
        return gameProfile;
    }

}
