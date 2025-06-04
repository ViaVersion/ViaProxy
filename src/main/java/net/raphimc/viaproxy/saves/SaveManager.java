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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.lenni0451.reflect.stream.RStream;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.saves.impl.AccountsSaveV3;
import net.raphimc.viaproxy.saves.impl.UISave;
import net.raphimc.viaproxy.util.logging.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class SaveManager {

    private static final File SAVE_FILE = new File(ViaProxy.getCwd(), "saves.json");
    private static final Gson GSON = new Gson();

    public final AccountsSaveV3 accountsSave = new AccountsSaveV3();
    public final UISave uiSave = new UISave();

    public SaveManager() {
        try {
            if (!SAVE_FILE.exists()) {
                SAVE_FILE.createNewFile();
                this.save();
            }

            final FileReader reader = new FileReader(SAVE_FILE);
            final JsonObject saveObject = GSON.fromJson(reader, JsonObject.class);
            reader.close();

            SaveMigrator.migrate(saveObject);

            RStream
                    .of(this)
                    .fields()
                    .filter(field -> AbstractSave.class.isAssignableFrom(field.type()))
                    .forEach(field -> {
                        final AbstractSave save = field.get();
                        try {
                            if (saveObject.has(save.getName())) {
                                save.load(saveObject.get(save.getName()));
                            }
                        } catch (Throwable e) {
                            Logger.LOGGER.error("Failed to load save " + save.getName(), e);
                        }
                    });
        } catch (Throwable e) {
            throw new RuntimeException("Failed to initialize SaveManager", e);
        }
    }

    public void save() {
        try {
            final JsonObject saveObject = new JsonObject();
            RStream
                    .of(this)
                    .fields()
                    .filter(field -> AbstractSave.class.isAssignableFrom(field.type()))
                    .forEach(field -> {
                        final AbstractSave save = field.get();
                        try {
                            final JsonElement saveData = save.save();
                            if (saveData != null) {
                                saveObject.add(save.getName(), saveData);
                            }
                        } catch (Throwable e) {
                            Logger.LOGGER.error("Failed to save save " + save.getName(), e);
                        }
                    });

            final FileWriter writer = new FileWriter(SAVE_FILE);
            GSON.toJson(saveObject, writer);
            writer.close();
        } catch (Throwable e) {
            Logger.LOGGER.error("Failed to save saves to file", e);
        }
    }

}
