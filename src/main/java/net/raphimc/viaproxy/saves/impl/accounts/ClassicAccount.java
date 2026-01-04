/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2026 RK_01/RaphiMC and contributors
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
package net.raphimc.viaproxy.saves.impl.accounts;

import com.google.gson.JsonObject;

public class ClassicAccount extends OfflineAccount {

    private final String mppass;

    public ClassicAccount(JsonObject jsonObject) {
        super(jsonObject);
        this.mppass = jsonObject.get("mppass").getAsString();
    }

    public ClassicAccount(final String name, final String mppass) {
        super(name);
        this.mppass = mppass;
    }

    @Override
    public JsonObject toJson() {
        final JsonObject jsonObject = super.toJson();
        jsonObject.addProperty("mppass", this.mppass);
        return jsonObject;
    }

    public String getMppass() {
        return this.mppass;
    }

}
