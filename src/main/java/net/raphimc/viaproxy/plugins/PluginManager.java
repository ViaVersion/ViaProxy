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
package net.raphimc.viaproxy.plugins;

import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.LambdaMetaFactoryGenerator;
import net.raphimc.viaproxy.util.logging.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PluginManager {

    public static final LambdaManager EVENT_MANAGER = LambdaManager.threadSafe(new LambdaMetaFactoryGenerator());

    private static final Yaml YAML = new Yaml();
    private static final File PLUGINS_DIR = new File("plugins");
    private static final List<ViaProxyPlugin> PLUGINS = new ArrayList<>();

    public static List<ViaProxyPlugin> getPlugins() {
        return Collections.unmodifiableList(PLUGINS);
    }

    public static void loadPlugins() {
        if (!PLUGINS_DIR.exists() || !PLUGINS_DIR.isDirectory()) {
            if (!PLUGINS_DIR.mkdirs()) {
                return;
            }
        }

        File[] files = PLUGINS_DIR.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (!file.getName().toLowerCase().endsWith(".jar")) continue;
            try {
                loadAndScanJar(file);
            } catch (Throwable t) {
                new Exception("Unable to load plugin '" + file.getName() + "'", t).printStackTrace();
            }
        }
    }

    private static void loadAndScanJar(final File file) throws Throwable {
        URLClassLoader loader = new URLClassLoader(new URL[]{new URL("jar:file:" + file.getAbsolutePath() + "!/")}, PluginManager.class.getClassLoader());
        InputStream viaproxyYml = loader.getResourceAsStream("viaproxy.yml");
        if (viaproxyYml == null) throw new IllegalStateException("Plugin '" + file.getName() + "' does not have a viaproxy.yml");
        Map<String, Object> yaml = YAML.load(viaproxyYml);
        if (!yaml.containsKey("name")) throw new IllegalStateException("Plugin '" + file.getName() + "' does not have a name attribute in the viaproxy.yml");
        if (!yaml.containsKey("author")) throw new IllegalStateException("Plugin '" + file.getName() + "' does not have a author attribute in the viaproxy.yml");
        if (!yaml.containsKey("version")) throw new IllegalStateException("Plugin '" + file.getName() + "' does not have a version attribute in the viaproxy.yml");
        if (!yaml.containsKey("main")) throw new IllegalStateException("Plugin '" + file.getName() + "' does not have a main attribute in the viaproxy.yml");

        String main = (String) yaml.get("main");

        Class<?> mainClass = loader.loadClass(main);
        if (!ViaProxyPlugin.class.isAssignableFrom(mainClass)) {
            throw new IllegalStateException("Class '" + mainClass.getName() + "' from '" + file.getName() + "' does not extend ViaProxyPlugin");
        }
        Object instance = mainClass.newInstance();
        ViaProxyPlugin plugin = (ViaProxyPlugin) instance;
        PLUGINS.add(plugin);

        plugin.onEnable();
        Logger.LOGGER.info("Successfully loaded plugin '" + yaml.get("name") + "' by " + yaml.get("author") + " (v" + yaml.get("version") + ")");
    }

}
