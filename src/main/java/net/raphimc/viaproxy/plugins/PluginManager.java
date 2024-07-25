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
package net.raphimc.viaproxy.plugins;

import com.vdurmont.semver4j.Semver;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.additionalclassprovider.GuavaClassPathProvider;
import net.lenni0451.classtransform.additionalclassprovider.LazyFileClassProvider;
import net.lenni0451.classtransform.utils.loader.InjectionClassLoader;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import net.lenni0451.reflect.stream.RStream;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.util.logging.Logger;
import org.objectweb.asm.Opcodes;
import org.yaml.snakeyaml.Yaml;
import xyz.wagyourtail.jvmdg.Constants;
import xyz.wagyourtail.jvmdg.runtime.ClassDowngradingAgent;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PluginManager {

    public static final File PLUGINS_DIR = new File(ViaProxy.getCwd(), "plugins");

    private final Yaml yaml = new Yaml();
    private final IClassProvider rootClassProvider = new GuavaClassPathProvider();
    private final List<ViaProxyPlugin> plugins = new ArrayList<>();

    public PluginManager() {
        this.loadPlugins();
        Runtime.getRuntime().addShutdownHook(new Thread(this::unloadPlugins));
    }

    public List<ViaProxyPlugin> getPlugins() {
        return Collections.unmodifiableList(this.plugins);
    }

    public ViaProxyPlugin getPlugin(String name) {
        for (ViaProxyPlugin plugin : this.plugins) {
            if (plugin.getName().equalsIgnoreCase(name)) {
                return plugin;
            }
        }

        return null;
    }

    private void loadPlugins() {
        if (!PLUGINS_DIR.exists() || !PLUGINS_DIR.isDirectory()) {
            if (!PLUGINS_DIR.mkdirs()) {
                return;
            }
        }

        final File[] files = PLUGINS_DIR.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (!file.getName().toLowerCase().endsWith(".jar")) continue;
            try {
                loadAndScanJar(file);
            } catch (Throwable e) {
                Logger.LOGGER.error("Unable to load plugin '" + file.getName() + "'", e);
            }
        }

        for (ViaProxyPlugin plugin : this.plugins) {
            if (!plugin.isEnabled()) {
                this.enablePlugin(plugin);
            }
        }
    }

    private void loadAndScanJar(final File file) throws Throwable {
        final URL url = file.toURI().toURL();
        final TransformerManager transformerManager = new TransformerManager(new LazyFileClassProvider(Collections.singletonList(file), this.rootClassProvider));
        final InjectionClassLoader classLoader = new InjectionClassLoader(transformerManager, PluginManager.class.getClassLoader(), url);
        classLoader.addProtectedPackage("io.netty.");

        try {
            final String[] versions = System.getProperty("java.class.version").split("\\.");
            final int nativeClassVersion = Integer.parseInt(versions[0]);
            if (nativeClassVersion < Opcodes.V17) {
                System.setProperty(Constants.ALLOW_MAVEN_LOOKUP, "false");
                transformerManager.addClassFileTransformer(classLoader, new ClassDowngradingAgent());
            }
        } catch (Throwable e) {
            Logger.LOGGER.error("Failed to setup class downgrading", e);
        }

        final InputStream viaproxyYml = classLoader.getResourceAsStream("viaproxy.yml");
        if (viaproxyYml == null) throw new IllegalStateException("Plugin '" + file.getName() + "' does not have a viaproxy.yml");
        final Map<String, Object> yaml = this.yaml.load(viaproxyYml);
        if (!yaml.containsKey("name")) throw new IllegalStateException("Plugin '" + file.getName() + "' does not have a name attribute in the viaproxy.yml");
        if (!yaml.containsKey("author")) throw new IllegalStateException("Plugin '" + file.getName() + "' does not have a author attribute in the viaproxy.yml");
        if (!yaml.containsKey("version")) throw new IllegalStateException("Plugin '" + file.getName() + "' does not have a version attribute in the viaproxy.yml");
        if (!yaml.containsKey("main")) throw new IllegalStateException("Plugin '" + file.getName() + "' does not have a main attribute in the viaproxy.yml");
        final Semver minVersion = new Semver(yaml.getOrDefault("min-version", "0.0.0").toString());
        if (!ViaProxy.VERSION.startsWith("${") && minVersion.isGreaterThan(ViaProxy.VERSION.replace("-SNAPSHOT", ""))) {
            throw new IllegalStateException("Plugin '" + file.getName() + "' requires a newer version of ViaProxy (v" + minVersion + ")");
        }

        final String main = (String) yaml.get("main");

        final Class<?> mainClass = classLoader.loadClass(main);
        if (!ViaProxyPlugin.class.isAssignableFrom(mainClass)) {
            throw new IllegalStateException("Class '" + mainClass.getName() + "' from '" + file.getName() + "' does not extend ViaProxyPlugin");
        }
        final Object instance = mainClass.getDeclaredConstructor().newInstance();
        final ViaProxyPlugin plugin = (ViaProxyPlugin) instance;

        plugin.init(classLoader, yaml);

        if (plugin.getDepends().size() > 1) {
            throw new IllegalStateException("Plugin '" + file.getName() + "' has more than one dependency. This is not supported yet.");
        }

        Logger.LOGGER.info("Loaded plugin '" + plugin.getName() + "' by " + plugin.getAuthor() + " (v" + plugin.getVersion() + ")");
        this.plugins.add(plugin);
    }

    private void enablePlugin(final ViaProxyPlugin plugin) {
        for (String depend : plugin.getDepends()) {
            final ViaProxyPlugin dependPlugin = this.getPlugin(depend);
            if (dependPlugin == null) {
                Logger.LOGGER.error("Plugin '" + plugin.getName() + "' depends on '" + depend + "' which is not loaded");
                return;
            }
            if (!dependPlugin.isEnabled()) {
                this.enablePlugin(dependPlugin);
            }

            RStream.of(plugin.getClassLoader()).withSuper().fields().by("parent").set(dependPlugin.getClassLoader());
        }

        try {
            plugin.enable();
            Logger.LOGGER.info("Enabled plugin '" + plugin.getName() + "'");
        } catch (Throwable e) {
            Logger.LOGGER.error("Failed to enable plugin '" + plugin.getName() + "'", e);
        }
    }

    private void unloadPlugins() {
        for (ViaProxyPlugin plugin : this.plugins) {
            if (plugin.isEnabled()) {
                this.disablePlugin(plugin);
            }
        }
    }

    private void disablePlugin(final ViaProxyPlugin plugin) {
        for (String depend : plugin.getDepends()) {
            final ViaProxyPlugin dependPlugin = this.getPlugin(depend);
            if (dependPlugin == null) {
                Logger.LOGGER.error("Plugin '" + plugin.getName() + "' depends on '" + depend + "' which is not loaded");
                return;
            }
            if (dependPlugin.isEnabled()) {
                this.disablePlugin(dependPlugin);
            }
        }

        try {
            plugin.disable();
            Logger.LOGGER.info("Disabled plugin '" + plugin.getName() + "'");
        } catch (Throwable e) {
            Logger.LOGGER.error("Failed to disable plugin '" + plugin.getName() + "'", e);
        }
    }

}
