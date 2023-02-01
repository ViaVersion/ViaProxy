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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.raphimc.mcauth.MinecraftAuth;
import net.raphimc.viaproxy.util.logging.Logger;

public class SaveMigrator {

    public static void migrate(final SaveManager saveManager, final JsonObject jsonObject) {
        try {
            if (jsonObject.has("accounts")) {
                for (JsonElement element : jsonObject.getAsJsonArray("accounts")) {
                    final JsonObject object = element.getAsJsonObject();
                    if (object.has("is_offline_mode_account") && object.get("is_offline_mode_account").getAsBoolean()) {
                        saveManager.accountsSave.addAccount(object.get("name").getAsString());
                    } else {
                        saveManager.accountsSave.addAccount(MinecraftAuth.Java.Title.MC_PROFILE.fromJson(object));
                    }
                }
            }
        } catch (Throwable e) {
            Logger.LOGGER.error("Failed to migrate accounts save", e);
        }
    }

}
