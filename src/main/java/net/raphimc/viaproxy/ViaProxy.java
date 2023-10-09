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
import net.lenni0451.reflect.Agents;
import net.lenni0451.reflect.ClassLoaders;
import net.lenni0451.reflect.Methods;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.connection.NetServer;
import net.raphimc.viaproxy.cli.ConsoleHandler;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.plugins.PluginManager;
import net.raphimc.viaproxy.plugins.events.Client2ProxyHandlerCreationEvent;
import net.raphimc.viaproxy.plugins.events.ProxyStartEvent;
import net.raphimc.viaproxy.plugins.events.ProxyStopEvent;
import net.raphimc.viaproxy.proxy.EventListener;
import net.raphimc.viaproxy.proxy.client2proxy.Client2ProxyChannelInitializer;
import net.raphimc.viaproxy.proxy.client2proxy.Client2ProxyHandler;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;
import net.raphimc.viaproxy.saves.SaveManager;
import net.raphimc.viaproxy.tasks.LoaderTask;
import net.raphimc.viaproxy.tasks.UpdateCheckTask;
import net.raphimc.viaproxy.ui.ViaProxyUI;
import net.raphimc.viaproxy.ui.events.UIInitEvent;
import net.raphimc.viaproxy.util.ClassLoaderPriorityUtil;
import net.raphimc.viaproxy.util.logging.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.lang.instrument.Instrumentation;

public class ViaProxy {

    public static final String VERSION = "${version}";
    public static final String IMPL_VERSION = "${impl_version}";

    private static Instrumentation instrumentation;
    public static SaveManager saveManager;
    public static NetServer currentProxyServer;
    public static ChannelGroup c2pChannels;
    public static ViaProxyUI ui;

    public static void agentmain(final String args, final Instrumentation instrumentation) {
        ViaProxy.instrumentation = instrumentation;
    }

    public static void main(String[] args) throws Throwable {
        final IClassProvider classProvider = new GuavaClassPathProvider();
        final TransformerManager transformerManager = new TransformerManager(classProvider);
        transformerManager.addTransformerPreprocessor(new MixinsTranslator());
        transformerManager.addTransformer("net.raphimc.viaproxy.injection.transformer.**");
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

    public static void injectedMain(final String injectionMethod, final String[] args) throws InterruptedException, IOException {
        Logger.setup();
        final boolean hasUI = args.length == 0 && !GraphicsEnvironment.isHeadless();
        ConsoleHandler.hookConsole();
        Logger.LOGGER.info("Initializing ViaProxy {} v{} (Injected using {})...", hasUI ? "GUI" : "CLI", VERSION, injectionMethod);
        Logger.LOGGER.info("Using java version: " + System.getProperty("java.vm.name") + " " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ") on " + System.getProperty("os.name"));
        Logger.LOGGER.info("Available memory (bytes): " + Runtime.getRuntime().maxMemory());

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

        ClassLoaderPriorityUtil.loadOverridingJars();
        loadNetty();
        saveManager = new SaveManager();
        PluginManager.loadPlugins();
        PluginManager.EVENT_MANAGER.register(EventListener.class);

        final Thread loaderThread = new Thread(new LoaderTask(), "ViaLoader");
        final Thread updateCheckThread = new Thread(new UpdateCheckTask(hasUI), "UpdateCheck");

        if (hasUI) {
            loaderThread.start();
            SwingUtilities.invokeLater(() -> {
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
            loaderThread.join();
            while (ui == null) {
                Logger.LOGGER.info("Waiting for UI to be initialized...");
                Thread.sleep(1000);
            }
            ViaProxyUI.EVENT_MANAGER.call(new UIInitEvent());
            Logger.LOGGER.info("ViaProxy started successfully!");
            return;
        }

        Options.parse(args);

        if (System.getProperty("skipUpdateCheck") == null) {
            updateCheckThread.start();
        }
        loaderThread.start();
        loaderThread.join();
        Logger.LOGGER.info("ViaProxy started successfully!");
        startProxy();

        while (true) {
            Thread.sleep(Integer.MAX_VALUE);
        }
    }

    public static void startProxy() {
        if (currentProxyServer != null) {
            throw new IllegalStateException("Proxy is already running");
        }
        try {
            Logger.LOGGER.info("Starting proxy server");
            currentProxyServer = new NetServer(() -> PluginManager.EVENT_MANAGER.call(new Client2ProxyHandlerCreationEvent(new Client2ProxyHandler(), false)).getHandler(), Client2ProxyChannelInitializer::new);
            PluginManager.EVENT_MANAGER.call(new ProxyStartEvent());
            Logger.LOGGER.info("Binding proxy server to " + Options.BIND_ADDRESS + ":" + Options.BIND_PORT);
            currentProxyServer.bind(Options.BIND_ADDRESS, Options.BIND_PORT, false);
        } catch (Throwable e) {
            currentProxyServer = null;
            throw e;
        }
    }

    public static void stopProxy() {
        if (currentProxyServer != null) {
            Logger.LOGGER.info("Stopping proxy server");
            PluginManager.EVENT_MANAGER.call(new ProxyStopEvent());

            currentProxyServer.getChannel().close();
            currentProxyServer = null;

            for (Channel channel : c2pChannels) {
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
        c2pChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    }

}
