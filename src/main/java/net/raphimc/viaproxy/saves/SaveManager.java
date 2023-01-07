package net.raphimc.viaproxy.saves;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.lenni0451.reflect.stream.RStream;
import net.raphimc.viaproxy.saves.impl.AccountsSave;
import net.raphimc.viaproxy.util.logging.Logger;

import java.io.*;

public class SaveManager {

    private static final File SAVE_FILE = new File("saves.json");
    private static final Gson GSON = new Gson();

    public final AccountsSave accountsSave = new AccountsSave();

    public SaveManager() {
        this.load();
    }

    public void load() {
        try {
            if (!SAVE_FILE.exists()) {
                SAVE_FILE.createNewFile();
                this.save();
            }

            final FileReader reader = new FileReader(SAVE_FILE);
            final JsonObject saveObject = GSON.fromJson(reader, JsonObject.class);
            reader.close();

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
            Logger.LOGGER.error("Failed to load saves from file", e);
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
                            saveObject.add(save.getName(), save.save());
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
