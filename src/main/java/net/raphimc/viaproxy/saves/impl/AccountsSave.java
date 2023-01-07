package net.raphimc.viaproxy.saves.impl;

import com.google.gson.*;
import net.raphimc.mcauth.MinecraftAuth;
import net.raphimc.mcauth.step.java.StepGameOwnership;
import net.raphimc.mcauth.step.java.StepMCProfile;
import net.raphimc.mcauth.util.MicrosoftConstants;
import net.raphimc.viaproxy.saves.AbstractSave;
import net.raphimc.viaproxy.util.logging.Logger;
import org.apache.http.impl.client.CloseableHttpClient;

import java.util.*;

public class AccountsSave extends AbstractSave {

    private List<StepMCProfile.MCProfile> accounts = new ArrayList<>();

    public AccountsSave() {
        super("accounts");
    }

    @Override
    public void load(JsonElement jsonElement) throws Exception {
        this.accounts = new ArrayList<>();
        for (JsonElement element : jsonElement.getAsJsonArray()) {
            final JsonObject object = element.getAsJsonObject();
            if (object.has("is_offline_mode_account") && object.get("is_offline_mode_account").getAsBoolean()) {
                this.addOfflineAccount(object.get("name").getAsString());
            } else {
                this.addAccount(MinecraftAuth.Java.Title.MC_PROFILE.fromJson(object));
            }
        }
    }

    @Override
    public JsonElement save() {
        final JsonArray array = new JsonArray();
        for (StepMCProfile.MCProfile account : this.accounts) {
            if (account.prevResult().items().isEmpty()) {
                final JsonObject object = new JsonObject();
                object.addProperty("is_offline_mode_account", true);
                object.addProperty("name", account.name());
                array.add(object);
            } else {
                array.add(account.toJson());
            }
        }
        return array;
    }

    public void addAccount(final StepMCProfile.MCProfile profile) {
        this.accounts.add(profile);
    }

    public void addAccount(final int index, final StepMCProfile.MCProfile profile) {
        this.accounts.add(index, profile);
    }

    public void addOfflineAccount(final String name) {
        this.addAccount(new StepMCProfile.MCProfile(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes()), name, null, new StepGameOwnership.GameOwnership(Collections.emptyList(), null)));
    }

    public void removeAccount(final StepMCProfile.MCProfile profile) {
        this.accounts.remove(profile);
    }

    public void refreshAccounts() {
        final List<StepMCProfile.MCProfile> accounts = new ArrayList<>();
        for (StepMCProfile.MCProfile account : this.accounts) {
            if (account.prevResult().items().isEmpty()) {
                accounts.add(account);
                continue;
            }

            try (final CloseableHttpClient httpClient = MicrosoftConstants.createHttpClient()) {
                accounts.add(MinecraftAuth.Java.Title.MC_PROFILE.refresh(httpClient, account));
            } catch (Throwable e) {
                Logger.LOGGER.error("Failed to refresh account " + account.name() + ", removing it from the list.", e);
            }
        }
        this.accounts = accounts;
    }

    public List<StepMCProfile.MCProfile> getAccounts() {
        return Collections.unmodifiableList(this.accounts);
    }

}
