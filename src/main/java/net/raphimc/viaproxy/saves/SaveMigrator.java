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
package net.raphimc.viaproxy.saves;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.raphimc.minecraftauth.util.MinecraftAuth4To5Migrator;
import net.raphimc.viaproxy.saves.impl.accounts.BedrockAccount;
import net.raphimc.viaproxy.saves.impl.accounts.MicrosoftAccount;
import net.raphimc.viaproxy.util.logging.Logger;

public class SaveMigrator {

    public static void migrate(final JsonObject jsonObject) {
        try { // Accounts V3 -> V4 (MinecraftAuth 4 -> 5 migration)
            if (jsonObject.has("accountsV3")) {
                final JsonArray oldAccountsArray = jsonObject.getAsJsonArray("accountsV3");
                final JsonArray newAccountsArray = new JsonArray(oldAccountsArray.size());
                for (JsonElement oldAccountElement : oldAccountsArray) {
                    final JsonObject oldAccountObject = oldAccountElement.getAsJsonObject();
                    final String type = oldAccountObject.get("accountType").getAsString();
                    final JsonObject newAccountObject;
                    if (MicrosoftAccount.class.getName().equals(type)) {
                        newAccountObject = MinecraftAuth4To5Migrator.migrateJavaSave(oldAccountObject);
                        newAccountObject.addProperty("accountType", type);
                    } else if (BedrockAccount.class.getName().equals(type)) {
                        newAccountObject = MinecraftAuth4To5Migrator.migrateBedrockSave(oldAccountObject);
                        newAccountObject.addProperty("accountType", type);
                    } else {
                        newAccountObject = oldAccountObject;
                    }
                    newAccountsArray.add(newAccountObject);
                }
                jsonObject.remove("accountsV3");
                jsonObject.add("accountsV4", newAccountsArray);
            }
        } catch (Throwable e) {
            Logger.LOGGER.error("Failed to migrate accounts save", e);
        }
    }

}
