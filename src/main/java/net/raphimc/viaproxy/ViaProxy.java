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
import net.lenni0451.reflect.Agents;
import net.lenni0451.reflect.ClassLoaders;
import net.lenni0451.reflect.JavaBypass;
import net.lenni0451.reflect.Methods;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.connection.NetServer;
import net.raphimc.viaproxy.cli.ConsoleHandler;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.plugins.PluginManager;
import net.raphimc.viaproxy.plugins.events.Client2ProxyHandlerCreationEvent;
import net.raphimc.viaproxy.plugins.events.ProxyStartEvent;
import net.raphimc.viaproxy.plugins.events.ProxyStopEvent;
import net.raphimc.viaproxy.plugins.events.ViaProxyLoadedEvent;
import net.raphimc.viaproxy.protocoltranslator.ProtocolTranslator;
import net.raphimc.viaproxy.proxy.client2proxy.Client2ProxyChannelInitializer;
import net.raphimc.viaproxy.proxy.client2proxy.Client2ProxyHandler;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;
import net.raphimc.viaproxy.saves.SaveManager;
import net.raphimc.viaproxy.tasks.UpdateCheckTask;
import net.raphimc.viaproxy.ui.ViaProxyUI;
import net.raphimc.viaproxy.ui.events.UIInitEvent;
import net.raphimc.viaproxy.util.AddressUtil;
import net.raphimc.viaproxy.util.ClassLoaderPriorityUtil;
import net.raphimc.viaproxy.util.logging.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;

public class ViaProxy {

    public static final String VERSION = "${version}";
    public static final String IMPL_VERSION = "${impl_version}";

    public static final LambdaManager EVENT_MANAGER = LambdaManager.threadSafe(new LambdaMetaFactoryGenerator(JavaBypass.TRUSTED_LOOKUP));
    private static /*final*/ SaveManager SAVE_MANAGER;
    private static /*final*/ PluginManager PLUGIN_MANAGER;
    private static /*final*/ ChannelGroup CLIENT_CHANNELS;

    private static Instrumentation instrumentation;
    private static NetServer currentProxyServer;
    private static ViaProxyUI ui;

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
        Logger.setup();

        final boolean hasUI = args.length == 0 && !GraphicsEnvironment.isHeadless();
        Logger.LOGGER.info("Initializing ViaProxy {} v{} ({}) (Injected using {})...", hasUI ? "GUI" : "CLI", VERSION, IMPL_VERSION, injectionMethod);
        Logger.LOGGER.info("Using java version: " + System.getProperty("java.vm.name") + " " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ") on " + System.getProperty("os.name"));
        Logger.LOGGER.info("Available memory (bytes): " + Runtime.getRuntime().maxMemory());
        Logger.LOGGER.info("Working directory: " + System.getProperty("user.dir"));

        if (System.getProperty("ignoreSystemRequirements") == null) {
            if ("32".equals(System.getProperty("sun.arch.data.model")) && Runtime.getRuntime().maxMemory() < 256 * 1024 * 1024) {
                Logger.LOGGER.fatal("ViaProxy is not able to run on 32bit Java.");
                if (hasUI) {
                    JOptionPane.showMessageDialog(null, "ViaProxy is not able to run on 32bit Java. Please install 64bit Java", "ViaProxy", JOptionPane.ERROR_MESSAGE);
                }
                System.exit(1);
            }

            if (Runtime.getRuntime().maxMemory() < 256 * 1024 * 1024) {
                Logger.LOGGER.fatal("ViaProxy is not able to run with less than 256MB of RAM.");
                if (hasUI) {
                    JOptionPane.showMessageDialog(null, "ViaProxy is not able to run with less than 256MB of RAM.", "ViaProxy", JOptionPane.ERROR_MESSAGE);
                }
                System.exit(1);
            } else if (Runtime.getRuntime().maxMemory() < 512 * 1024 * 1024) {
                Logger.LOGGER.warn("ViaProxy has less than 512MB of RAM. This may cause issues with multiple clients connected.");
                if (hasUI) {
                    JOptionPane.showMessageDialog(null, "ViaProxy has less than 512MB of RAM. This may cause issues with multiple clients connected.", "ViaProxy", JOptionPane.WARNING_MESSAGE);
                }
            }
        }

        ConsoleHandler.hookConsole();
        ClassLoaderPriorityUtil.loadOverridingJars();
        ViaProxy.loadNetty();
        ProtocolTranslator.init();

        SAVE_MANAGER = new SaveManager();
        PLUGIN_MANAGER = new PluginManager();

        final Thread updateCheckThread = new Thread(new UpdateCheckTask(hasUI), "UpdateCheck");

        if (hasUI) {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    ui = new ViaProxyUI();
                } catch (Throwable e) {
                    Logger.LOGGER.fatal("Failed to initialize UI", e);
                    System.exit(1);
                }
            });
            if (System.getProperty("skipUpdateCheck") == null) {
                updateCheckThread.start();
            }
            ui.eventManager.call(new UIInitEvent());
            EVENT_MANAGER.call(new ViaProxyLoadedEvent());
            Logger.LOGGER.info("ViaProxy started successfully!");
        } else {
            Options.parse(args);

            if (System.getProperty("skipUpdateCheck") == null) {
                updateCheckThread.start();
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
            Logger.LOGGER.info("Binding proxy server to " + AddressUtil.toString(Options.BIND_ADDRESS));
            currentProxyServer.bind(Options.BIND_ADDRESS, false);
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

    public static SaveManager getSaveManager() {
        return SAVE_MANAGER;
    }

    public static PluginManager getPluginManager() {
        return PLUGIN_MANAGER;
    }

    public static ChannelGroup getConnectedClients() {
        return CLIENT_CHANNELS;
    }

    public static NetServer getCurrentProxyServer() {
        return currentProxyServer;
    }

    public static ViaProxyUI getUI() {
        return ui;
    }

}
