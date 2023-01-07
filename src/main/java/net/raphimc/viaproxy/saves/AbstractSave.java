package net.raphimc.viaproxy.saves;

import com.google.gson.JsonElement;

public abstract class AbstractSave {

    private final String name;

    public AbstractSave(final String name) {
        this.name = name;
    }

    public abstract void load(final JsonElement jsonElement) throws Throwable;

    public abstract JsonElement save() throws Throwable;

    public String getName() {
        return this.name;
    }

}
