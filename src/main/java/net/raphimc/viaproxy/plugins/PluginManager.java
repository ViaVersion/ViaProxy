package net.raphimc.viaproxy.plugins;

import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.LambdaMetaFactoryGenerator;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class PluginManager {

    public static final LambdaManager EVENT_MANAGER = LambdaManager.threadSafe(new LambdaMetaFactoryGenerator());

    private static final Yaml YAML = new Yaml();
    private static final File PLUGINS_DIR = new File("plugins");
    private static final List<ViaProxyPlugin> PLUGINS = new ArrayList<>();

    public static void loadPlugins() {
        if (!PLUGINS_DIR.exists() || !PLUGINS_DIR.isDirectory()) return;

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
        if (viaproxyYml == null)
            throw new IllegalStateException("Plugin '" + file.getName() + "' does not have a viaproxy.yml");
        Map<String, Object> yaml = YAML.load(viaproxyYml);
        if (!yaml.containsKey("name"))
            throw new IllegalStateException("Plugin '" + file.getName() + "' does not have a name attribute in the viaproxy.yml");
        if (!yaml.containsKey("author"))
            throw new IllegalStateException("Plugin '" + file.getName() + "' does not have a author attribute in the viaproxy.yml");
        if (!yaml.containsKey("version"))
            throw new IllegalStateException("Plugin '" + file.getName() + "' does not have a version attribute in the viaproxy.yml");
        if (!yaml.containsKey("main"))
            throw new IllegalStateException("Plugin '" + file.getName() + "' does not have a main attribute in the viaproxy.yml");

        String main = (String) yaml.get("main");

        Class<?> mainClass = loader.loadClass(main);
        if (!ViaProxyPlugin.class.isAssignableFrom(mainClass))
            throw new IllegalStateException("Class '" + mainClass.getName() + "' from '" + file.getName() + "' does not extend ViaProxyPlugin");
        Object instance = mainClass.newInstance();
        ViaProxyPlugin plugin = (ViaProxyPlugin) instance;
        PLUGINS.add(plugin);

        plugin.onEnable();
    }

}
