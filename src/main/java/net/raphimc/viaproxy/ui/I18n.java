/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2024 RK_01/RaphiMC and contributors
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
package net.raphimc.viaproxy.ui;

import net.raphimc.viabedrock.api.util.FileSystemUtil;
import net.raphimc.viaproxy.ViaProxy;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class I18n {

    private static final String DEFAULT_LOCALE = "en_US";
    private static Map<String, Properties> LOCALES = new LinkedHashMap<>();
    private static String currentLocale;

    static {
        if (ViaProxy.getSaveManager() == null) {
            throw new IllegalStateException("ViaProxy is not yet initialized");
        }

        try {
            for (Map.Entry<Path, byte[]> entry : FileSystemUtil.getFilesInDirectory("assets/viaproxy/language").entrySet()) {
                final Properties properties = new Properties();
                properties.load(new InputStreamReader(new ByteArrayInputStream(entry.getValue()), StandardCharsets.UTF_8));
                if (properties.isEmpty()) continue;
                LOCALES.put(entry.getKey().getFileName().toString().replace(".properties", ""), properties);
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed to load translations", e);
        }
        LOCALES = LOCALES.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getValue().getProperty("language.name")))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> newValue, LinkedHashMap::new));

        currentLocale = ViaProxy.getSaveManager().uiSave.get("locale");
        if (currentLocale == null || !LOCALES.containsKey(currentLocale)) {
            final String systemLocale = Locale.getDefault().getLanguage() + '_' + Locale.getDefault().getCountry();
            if (LOCALES.containsKey(systemLocale)) {
                currentLocale = systemLocale;
            } else {
                for (Map.Entry<String, Properties> entry : LOCALES.entrySet()) {
                    if (entry.getKey().startsWith(Locale.getDefault().getLanguage() + '_')) {
                        currentLocale = entry.getKey();
                        break;
                    }
                }
            }
        }

        final int totalTranslation = LOCALES.get(DEFAULT_LOCALE).size();
        for (Properties properties : LOCALES.values()) {
            final int translated = properties.size();
            final float percentage = (float) translated / totalTranslation * 100;
            properties.put("language.completion", (int) Math.floor(percentage) + "%");
        }
    }

    public static String get(final String key) {
        return getSpecific(currentLocale, key);
    }

    public static String get(final String key, String... args) {
        return String.format(getSpecific(currentLocale, key), (Object[]) args);
    }

    public static String getSpecific(final String locale, final String key) {
        Properties properties = LOCALES.get(locale);
        if (properties == null) {
            properties = LOCALES.get(DEFAULT_LOCALE);
        }
        String value = properties.getProperty(key);
        if (value == null) {
            value = LOCALES.get(DEFAULT_LOCALE).getProperty(key);
        }
        if (value == null) {
            return "Missing translation for key: " + key;
        }
        return value;
    }

    public static String getCurrentLocale() {
        return currentLocale;
    }

    public static void setLocale(final String locale) {
        if (ViaProxy.getSaveManager() == null) {
            throw new IllegalStateException("ViaProxy is not yet initialized");
        }

        currentLocale = locale;
        ViaProxy.getSaveManager().uiSave.put("locale", locale);
        ViaProxy.getSaveManager().save();
    }

    public static Collection<String> getAvailableLocales() {
        return LOCALES.keySet();
    }

}
