package net.raphimc.viaproxy.saves.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.raphimc.mcauth.MinecraftAuth;
import net.raphimc.mcauth.step.java.StepMCProfile;
import net.raphimc.mcauth.util.MicrosoftConstants;
import net.raphimc.viaproxy.saves.AbstractSave;
import net.raphimc.viaproxy.util.logging.Logger;
import org.apache.http.impl.client.CloseableHttpClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AccountsSave extends AbstractSave {

    private List<StepMCProfile.MCProfile> accounts = new ArrayList<>();

    public AccountsSave() {
        super("accounts");
    }

    @Override
    public void load(JsonElement jsonElement) throws Exception {
        this.accounts = new ArrayList<>();
        for (JsonElement element : jsonElement.getAsJsonArray()) {
            this.accounts.add(MinecraftAuth.Java.Title.MC_PROFILE.fromJson(element.getAsJsonObject()));
        }
    }

    @Override
    public JsonElement save() {
        final JsonArray array = new JsonArray();
        for (StepMCProfile.MCProfile account : this.accounts) {
            array.add(account.toJson());
        }
        return array;
    }

    public void addAccount(final StepMCProfile.MCProfile profile) {
        this.accounts.add(profile);
    }

    public void addAccount(final int index, final StepMCProfile.MCProfile profile) {
        this.accounts.add(index, profile);
    }

    public void removeAccount(final StepMCProfile.MCProfile profile) {
        this.accounts.remove(profile);
    }

    public void refreshAccounts() {
        final List<StepMCProfile.MCProfile> accounts = new ArrayList<>();
        for (StepMCProfile.MCProfile account : this.accounts) {
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
