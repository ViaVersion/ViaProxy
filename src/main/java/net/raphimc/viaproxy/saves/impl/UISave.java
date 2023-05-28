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
package net.raphimc.viaproxy.saves.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.raphimc.viaproxy.saves.AbstractSave;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class UISave extends AbstractSave {

    private final Map<String, String> values;

    public UISave() {
        super("ui");

        this.values = new HashMap<>();
    }

    @Override
    public void load(JsonElement jsonElement) {
        this.values.clear();
        for (Map.Entry<String, JsonElement> entry : jsonElement.getAsJsonObject().entrySet()) this.values.put(entry.getKey(), entry.getValue().getAsString());
    }

    @Override
    public JsonElement save() {
        JsonObject jsonObject = new JsonObject();
        for (Map.Entry<String, String> entry : this.values.entrySet()) jsonObject.addProperty(entry.getKey(), entry.getValue());
        return jsonObject;
    }

    public void put(final String key, final String value) {
        this.values.put(key, value);
    }

    public String get(final String key) {
        return this.values.get(key);
    }

    public void loadTextField(final String key, final JTextField textField) {
        try {
            String value = this.values.get(key);
            if (value != null) textField.setText(value);
        } catch (Throwable ignored) {
        }
    }

    public void loadComboBox(final String key, final JComboBox<?> comboBox) {
        try {
            int index = Integer.parseInt(this.values.get(key));
            if (index >= 0 && index < comboBox.getItemCount()) comboBox.setSelectedIndex(index);
        } catch (Throwable ignored) {
        }
    }

    public void loadSpinner(final String key, final JSpinner spinner) {
        try {
            Integer value = Integer.valueOf(this.values.get(key));
            if (spinner.getModel() instanceof SpinnerNumberModel) {
                SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
                Comparable<Integer> minimum = (Comparable<Integer>) model.getMinimum();
                Comparable<Integer> maximum = (Comparable<Integer>) model.getMaximum();
                if (minimum.compareTo(value) <= 0 && maximum.compareTo(value) >= 0) spinner.setValue(value);
            } else {
                spinner.setValue(value);
            }
        } catch (Throwable ignored) {
        }
    }

    public void loadCheckBox(final String key, final JCheckBox checkBox) {
        try {
            boolean value = Boolean.parseBoolean(this.values.get(key));
            checkBox.setSelected(value);
        } catch (Throwable ignored) {
        }
    }

}
