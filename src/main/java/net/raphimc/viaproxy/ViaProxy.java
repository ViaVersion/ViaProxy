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
package net.raphimc.viaproxy;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.GlobalEventExecutor;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.additionalclassprovider.GuavaClassPathProvider;
import net.lenni0451.classtransform.mixinstranslator.MixinsTranslator;
import net.lenni0451.classtransform.utils.loader.EnumLoaderPriority;
import net.lenni0451.classtransform.utils.loader.InjectionClassLoader;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.LambdaMetaFactoryGenerator;
import net.lenni0451.optconfig.ConfigLoader;
import net.lenni0451.optconfig.provider.ConfigProvider;
import net.lenni0451.reflect.Agents;
import net.lenni0451.reflect.ClassLoaders;
import net.lenni0451.reflect.JavaBypass;
import net.lenni0451.reflect.Methods;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.connection.NetServer;
import net.raphimc.viaproxy.cli.ConsoleHandler;
import net.raphimc.viaproxy.plugins.PluginManager;
import net.raphimc.viaproxy.plugins.events.Client2ProxyHandlerCreationEvent;
import net.raphimc.viaproxy.plugins.events.ProxyStartEvent;
import net.raphimc.viaproxy.plugins.events.ProxyStopEvent;
import net.raphimc.viaproxy.plugins.events.ViaProxyLoadedEvent;
import net.raphimc.viaproxy.protocoltranslator.ProtocolTranslator;
import net.raphimc.viaproxy.protocoltranslator.viaproxy.ViaProxyConfig;
import net.raphimc.viaproxy.proxy.client2proxy.Client2ProxyChannelInitializer;
import net.raphimc.viaproxy.proxy.client2proxy.Client2ProxyHandler;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;
import net.raphimc.viaproxy.saves.SaveManager;
import net.raphimc.viaproxy.tasks.UpdateCheckTask;
import net.raphimc.viaproxy.ui.SplashScreen;
import net.raphimc.viaproxy.ui.ViaProxyWindow;
import net.raphimc.viaproxy.util.AddressUtil;
import net.raphimc.viaproxy.util.ClassLoaderPriorityUtil;
import net.raphimc.viaproxy.util.JarUtil;
import net.raphimc.viaproxy.util.logging.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ViaProxy {

    public static final String VERSION = "${version}";
    public static final String IMPL_VERSION = "${impl_version}";

    public static final LambdaManager EVENT_MANAGER = LambdaManager.threadSafe(new LambdaMetaFactoryGenerator(JavaBypass.TRUSTED_LOOKUP));
    private static /*final*/ File CWD;
    private static /*final*/ PluginManager PLUGIN_MANAGER;
    private static /*final*/ SaveManager SAVE_MANAGER;
    private static /*final*/ ViaProxyConfig CONFIG;
    private static /*final*/ ChannelGroup CLIENT_CHANNELS;

    private static Instrumentation instrumentation;
    private static NetServer currentProxyServer;
    private static ViaProxyWindow viaProxyWindow;
    private static JFrame foregroundWindow;

    public static void agentmain(final String args, final Instrumentation instrumentation) {
        ViaProxy.instrumentation = instrumentation;
    }

    public static void main(String[] args) throws Throwable {
        final IClassProvider classProvider = new GuavaClassPathProvider();
        final TransformerManager transformerManager = new TransformerManager(classProvider);
        transformerManager.addTransformerPreprocessor(new MixinsTranslator());
        transformerManager.addTransformer("net.raphimc.viaproxy.injection.mixins.**");
        if (instrumentation != null) {
            transformerManager.hookInstrumentation(instrumentation);
            injectedMain("Launcher Agent", args);
            return;
        }
        try {
            transformerManager.hookInstrumentation(Agents.getInstrumentation());
        } catch (Throwable t) {
            final InjectionClassLoader injectionClassLoader = new InjectionClassLoader(transformerManager, ClassLoaders.getSystemClassPath());
            injectionClassLoader.setPriority(EnumLoaderPriority.PARENT_FIRST);
            Thread.currentThread().setContextClassLoader(injectionClassLoader);
            Methods.invoke(null, Methods.getDeclaredMethod(injectionClassLoader.loadClass(ViaProxy.class.getName()), "injectedMain", String.class, String[].class), "Injection ClassLoader", args);
            return;
        }
        injectedMain("Runtime Agent", args);
    }

    public static void injectedMain(final String injectionMethod, final String[] args) throws InterruptedException, IOException, InvocationTargetException {
        final boolean useUI = args.length == 0 && !GraphicsEnvironment.isHeadless();
        final boolean useConfig = args.length == 2 && args[0].equals("config");
        final boolean useCLI = args.length > 0 && args[0].equals("cli");

        final List<File> potentialCwds = new ArrayList<>();
        if (System.getenv("VP_RUN_DIR") != null) {
            potentialCwds.add(new File(System.getenv("VP_RUN_DIR")));
        }
        potentialCwds.add(new File(System.getProperty("user.dir")));
        potentialCwds.add(new File("."));
        JarUtil.getJarFile().map(File::getParentFile).ifPresent(potentialCwds::add);

        final List<File> failedCwds = new ArrayList<>();
        for (File potentialCwd : potentialCwds) {
            if (potentialCwd.isDirectory()) {
                if (Files.isWritable(potentialCwd.toPath())) {
                    CWD = potentialCwd;
                    break;
                }
            }
            failedCwds.add(potentialCwd);
        }
        if (CWD != null) {
            System.setProperty("user.dir", CWD.getAbsolutePath());
        } else if (useUI) {
            JOptionPane.showMessageDialog(null, "Could not find a suitable directory to use as working directory. Make sure that the current folder is writeable.", "ViaProxy", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        } else {
            System.err.println("Could not find a suitable directory to use as working directory. Make sure that the current folder is writeable.");
            System.err.println("Attempted to use the following directories:");
            for (File failedCwd : failedCwds) {
                System.err.println("\t- " + failedCwd.getAbsolutePath());
            }
            System.exit(1);
        }

        Logger.setup();
        if (!useUI && !useConfig && !useCLI) {
            final String fileName = JarUtil.getJarFile().map(File::getName).orElse("ViaProxy.jar");
            Logger.LOGGER.info("Usage: java -jar " + fileName + " | Starts ViaProxy in graphical mode if available");
            Logger.LOGGER.info("Usage: java -jar " + fileName + " config <config file> | Starts ViaProxy with the specified config file");
            Logger.LOGGER.info("Usage: java -jar " + fileName + " cli --help | Starts ViaProxy in CLI mode");
            System.exit(1);
        }

        Logger.LOGGER.info("Initializing ViaProxy {} v{} ({}) (Injected using {})...", useUI ? "GUI" : "CLI", VERSION, IMPL_VERSION, injectionMethod);
        Logger.LOGGER.info("Using java version: " + System.getProperty("java.vm.name") + " " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ") on " + System.getProperty("os.name"));
        Logger.LOGGER.info("Available memory (bytes): " + Runtime.getRuntime().maxMemory());
        Logger.LOGGER.info("Working directory: " + CWD.getAbsolutePath());
        if (!failedCwds.isEmpty()) {
            Logger.LOGGER.warn("Failed to use the following directories as working directory:");
            for (File failedCwd : failedCwds) {
                Logger.LOGGER.warn("\t- " + failedCwd.getAbsolutePath());
            }
        }
        if (System.getProperty("ignoreSystemRequirements") == null) {
            if ("32".equals(System.getProperty("sun.arch.data.model")) && Runtime.getRuntime().maxMemory() < 256 * 1024 * 1024) {
                Logger.LOGGER.fatal("ViaProxy is not able to run on 32bit Java.");
                if (useUI) {
                    JOptionPane.showMessageDialog(null, "ViaProxy is not able to run on 32bit Java. Please install 64bit Java", "ViaProxy", JOptionPane.ERROR_MESSAGE);
                }
                System.exit(1);
            }

            if (Runtime.getRuntime().maxMemory() < 256 * 1024 * 1024) {
                Logger.LOGGER.fatal("ViaProxy is not able to run with less than 256MB of RAM.");
                if (useUI) {
                    JOptionPane.showMessageDialog(null, "ViaProxy is not able to run with less than 256MB of RAM.", "ViaProxy", JOptionPane.ERROR_MESSAGE);
                }
                System.exit(1);
            } else if (Runtime.getRuntime().maxMemory() < 512 * 1024 * 1024) {
                Logger.LOGGER.warn("ViaProxy has less than 512MB of RAM. This may cause issues with multiple clients connected.");
                if (useUI) {
                    JOptionPane.showMessageDialog(null, "ViaProxy has less than 512MB of RAM. This may cause issues with multiple clients connected.", "ViaProxy", JOptionPane.WARNING_MESSAGE);
                }
            }
        }

        final SplashScreen splashScreen;
        final Consumer<String> progressConsumer;
        if (useUI) {
            final float progressStep = 1F / 7F;
            foregroundWindow = splashScreen = new SplashScreen();
            progressConsumer = (text) -> {
                splashScreen.setProgress(splashScreen.getProgress() + progressStep);
                splashScreen.setText(text);
            };
            Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
                ViaProxyWindow.showException(e);
                System.exit(1);
            });
        } else {
            splashScreen = null;
            progressConsumer = text -> {
            };
        }
        progressConsumer.accept("Initializing ViaProxy");

        ConsoleHandler.hookConsole();
        ViaProxy.loadNetty();
        ClassLoaderPriorityUtil.loadOverridingJars();

        final File viaProxyConfigFile;
        if (useConfig) {
            viaProxyConfigFile = new File(args[1]);
        } else {
            viaProxyConfigFile = new File(ViaProxy.getCwd(), "viaproxy.yml");
        }
        final boolean firstStart = !viaProxyConfigFile.exists();

        progressConsumer.accept("Loading Plugins");
        PLUGIN_MANAGER = new PluginManager();
        progressConsumer.accept("Loading Protocol Translators");
        ProtocolTranslator.init();
        progressConsumer.accept("Loading Saves");
        SAVE_MANAGER = new SaveManager();
        progressConsumer.accept("Loading Config");
        final ConfigLoader<ViaProxyConfig> configLoader = new ConfigLoader<>(ViaProxyConfig.class);
        configLoader.getConfigOptions().setResetInvalidOptions(true).setRewriteConfig(true).setCommentSpacing(1);
        try {
            CONFIG = configLoader.load(ConfigProvider.file(viaProxyConfigFile)).getConfigInstance();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to load config", e);
        }

        if (useUI) {
            progressConsumer.accept("Loading GUI");
            SwingUtilities.invokeAndWait(() -> {
                try {
                    foregroundWindow = viaProxyWindow = new ViaProxyWindow();
                    progressConsumer.accept("Done");
                    splashScreen.dispose();
                } catch (Throwable e) {
                    Logger.LOGGER.fatal("Failed to initialize UI", e);
                    System.exit(1);
                }
            });
            if (System.getProperty("skipUpdateCheck") == null) {
                CompletableFuture.runAsync(new UpdateCheckTask(true));
            }
            EVENT_MANAGER.call(new ViaProxyLoadedEvent());
            Logger.LOGGER.info("ViaProxy started successfully!");
        } else {
            if (useCLI) {
                final String[] cliArgs = new String[args.length - 1];
                System.arraycopy(args, 1, cliArgs, 0, cliArgs.length);
                try {
                    CONFIG.loadFromArguments(cliArgs);
                } catch (Throwable e) {
                    throw new RuntimeException("Failed to load CLI arguments", e);
                }
            } else if (firstStart) {
                Logger.LOGGER.info("This is the first start of ViaProxy. Please configure the settings in the " + viaProxyConfigFile.getName() + " file and restart ViaProxy.");
                System.exit(0);
            }

            if (System.getProperty("skipUpdateCheck") == null) {
                CompletableFuture.runAsync(new UpdateCheckTask(false));
            }
            EVENT_MANAGER.call(new ViaProxyLoadedEvent());
            Logger.LOGGER.info("ViaProxy started successfully!");
            startProxy();

            while (true) {
                Thread.sleep(Integer.MAX_VALUE);
            }
        }
    }

    public static void startProxy() {
        if (currentProxyServer != null) {
            throw new IllegalStateException("Proxy is already running");
        }
        try {
            Logger.LOGGER.info("Starting proxy server");
            currentProxyServer = new NetServer(() -> EVENT_MANAGER.call(new Client2ProxyHandlerCreationEvent(new Client2ProxyHandler(), false)).getHandler(), Client2ProxyChannelInitializer::new);
            EVENT_MANAGER.call(new ProxyStartEvent());
            Logger.LOGGER.info("Binding proxy server to " + AddressUtil.toString(CONFIG.getBindAddress()));
            currentProxyServer.bind(CONFIG.getBindAddress(), false);
        } catch (Throwable e) {
            currentProxyServer = null;
            throw e;
        }
    }

    public static void stopProxy() {
        if (currentProxyServer != null) {
            Logger.LOGGER.info("Stopping proxy server");
            EVENT_MANAGER.call(new ProxyStopEvent());

            currentProxyServer.getChannel().close();
            currentProxyServer = null;

            for (Channel channel : CLIENT_CHANNELS) {
                try {
                    ProxyConnection.fromChannel(channel).kickClient("§cViaProxy has been stopped");
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static void loadNetty() {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
        if (System.getProperty("io.netty.allocator.maxOrder") == null) {
            System.setProperty("io.netty.allocator.maxOrder", "9");
        }
        MCPipeline.useOptimizedPipeline();
        CLIENT_CHANNELS = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    }

    public static File getCwd() {
        return CWD;
    }

    public static PluginManager getPluginManager() {
        return PLUGIN_MANAGER;
    }

    public static SaveManager getSaveManager() {
        return SAVE_MANAGER;
    }

    public static ViaProxyConfig getConfig() {
        return CONFIG;
    }

    public static ChannelGroup getConnectedClients() {
        return CLIENT_CHANNELS;
    }

    public static NetServer getCurrentProxyServer() {
        return currentProxyServer;
    }

    public static ViaProxyWindow getViaProxyWindow() {
        return viaProxyWindow;
    }

    public static JFrame getForegroundWindow() {
        return foregroundWindow;
    }

}
