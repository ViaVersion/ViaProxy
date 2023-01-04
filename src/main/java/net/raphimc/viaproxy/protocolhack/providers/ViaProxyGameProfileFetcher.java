package net.raphimc.viaproxy.protocolhack.providers;

import com.github.steveice10.mc.auth.exception.profile.ProfileException;
import com.github.steveice10.mc.auth.exception.profile.ProfileNotFoundException;
import com.github.steveice10.mc.auth.service.ProfileService;
import com.github.steveice10.mc.auth.service.SessionService;
import net.raphimc.vialegacy.protocols.release.protocol1_8to1_7_6_10.model.GameProfile;
import net.raphimc.vialegacy.protocols.release.protocol1_8to1_7_6_10.providers.GameProfileFetcher;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ViaProxyGameProfileFetcher extends GameProfileFetcher {

    public static SessionService sessionService = new SessionService();
    public static ProfileService profileService = new ProfileService();

    @Override
    public UUID loadMojangUUID(String playerName) throws ExecutionException, InterruptedException {
        final CompletableFuture<com.github.steveice10.mc.auth.data.GameProfile> future = new CompletableFuture<>();
        profileService.findProfilesByName(new String[]{playerName}, new ProfileService.ProfileLookupCallback() {
            @Override
            public void onProfileLookupSucceeded(com.github.steveice10.mc.auth.data.GameProfile profile) {
                future.complete(profile);
            }

            @Override
            public void onProfileLookupFailed(com.github.steveice10.mc.auth.data.GameProfile profile, Exception e) {
                future.completeExceptionally(e);
            }
        });
        if (!future.isDone()) {
            future.completeExceptionally(new ProfileNotFoundException());
        }
        return future.get().getId();
    }

    @Override
    public GameProfile loadGameProfile(UUID uuid) throws ProfileException {
        final com.github.steveice10.mc.auth.data.GameProfile inProfile = new com.github.steveice10.mc.auth.data.GameProfile(uuid, null);
        final com.github.steveice10.mc.auth.data.GameProfile mojangProfile = sessionService.fillProfileProperties(inProfile);

        final GameProfile gameProfile = new GameProfile(mojangProfile.getName(), mojangProfile.getId());
        for (com.github.steveice10.mc.auth.data.GameProfile.Property prop : mojangProfile.getProperties()) {
            gameProfile.addProperty(new GameProfile.Property(prop.getName(), prop.getValue(), prop.getSignature()));
        }
        return gameProfile;
    }

}
