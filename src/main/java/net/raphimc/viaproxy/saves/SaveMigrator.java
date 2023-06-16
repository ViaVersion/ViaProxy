/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
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
package net.raphimc.viaproxy.saves;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.raphimc.viaproxy.saves.impl.accounts.BedrockAccount;
import net.raphimc.viaproxy.saves.impl.accounts.MicrosoftAccount;
import net.raphimc.viaproxy.util.logging.Logger;

public class SaveMigrator {

    public static void migrate(final JsonObject jsonObject) {
        try {
            if (jsonObject.has("new_accounts")) {
                final JsonArray accountsArray = jsonObject.getAsJsonArray("new_accounts");
                for (int i = 0; i < accountsArray.size(); i++) {
                    final JsonObject object = accountsArray.get(i).getAsJsonObject();
                    final String type = object.get("account_type").getAsString();
                    if (BedrockAccount.class.getName().equals(type) && !object.has("mc_chain")) {
                        final JsonObject newObject = new JsonObject();
                        object.remove("account_type");
                        newObject.add("mc_chain", object);
                        newObject.addProperty("account_type", type);
                        accountsArray.set(i, newObject);
                    }
                    if (MicrosoftAccount.class.getName().equals(type) && !object.has("mc_profile")) {
                        final JsonObject newObject = new JsonObject();
                        object.remove("account_type");
                        newObject.add("mc_profile", object);
                        newObject.addProperty("account_type", type);
                        accountsArray.set(i, newObject);
                    }
                }
            }
        } catch (Throwable e) {
            Logger.LOGGER.error("Failed to migrate accounts save", e);
        }
    }

}
