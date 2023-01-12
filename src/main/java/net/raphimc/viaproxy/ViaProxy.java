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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import net.lenni0451.reflect.ClassLoaders;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.connection.NetServer;
import net.raphimc.viaproxy.cli.ConsoleHandler;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.injection.Java17ToJava8;
import net.raphimc.viaproxy.plugins.PluginManager;
import net.raphimc.viaproxy.protocolhack.ProtocolHack;
import net.raphimc.viaproxy.proxy.ProxyConnection;
import net.raphimc.viaproxy.proxy.client2proxy.Client2ProxyChannelInitializer;
import net.raphimc.viaproxy.proxy.client2proxy.Client2ProxyHandler;
import net.raphimc.viaproxy.saves.SaveManager;
import net.raphimc.viaproxy.ui.ViaProxyUI;
import net.raphimc.viaproxy.util.logging.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ViaProxy {

    public static final String VERSION = "${version}";

    public static SaveManager saveManager;
    public static NetServer currentProxyServer;
    public static ChannelGroup c2pChannels;

    public static void main(String[] args) throws Throwable {
        final IClassProvider classProvider = new GuavaClassPathProvider();
        final TransformerManager transformerManager = new TransformerManager(classProvider);
        transformerManager.addTransformerPreprocessor(new MixinsTranslator());
        transformerManager.addBytecodeTransformer(new Java17ToJava8(classProvider));
        transformerManager.addTransformer("net.raphimc.viaproxy.injection.transformer.**");
        transformerManager.addTransformer("net.raphimc.viaproxy.injection.mixins.**");
        final InjectionClassLoader injectionClassLoader = new InjectionClassLoader(transformerManager, ClassLoaders.getSystemClassPath());
        injectionClassLoader.addProtectedPackage("com.sun.");
        injectionClassLoader.setPriority(EnumLoaderPriority.PARENT_FIRST);
        injectionClassLoader.executeMain(ViaProxy.class.getName(), "injectedMain", args);
    }

    public static void injectedMain(String[] args) throws InterruptedException {
        Logger.setup();
        final boolean hasUI = args.length == 0 && !GraphicsEnvironment.isHeadless();
        ConsoleHandler.hookConsole();
        Logger.LOGGER.info("Initializing ViaProxy v" + VERSION + "...");
        saveManager = new SaveManager();
        setNettyParameters();
        MCPipeline.useOptimizedPipeline();
        c2pChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        Thread loaderThread = new Thread(() -> {
            ProtocolHack.init();
            PluginManager.loadPlugins();
        }, "ViaProtocolHack-Loader");
        Thread accountRefreshThread = new Thread(() -> {
            saveManager.accountsSave.refreshAccounts();
            saveManager.save();
        }, "AccountRefresh");
        Thread updateCheckThread = new Thread(() -> {
            if (VERSION.startsWith("$")) return; // Dev env check
            try {
                URL url = new URL("https://api.github.com/repos/RaphiMC/ViaProxy/releases/latest");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("User-Agent", "ViaProxy/" + VERSION);
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);

                InputStream in = con.getInputStream();
                byte[] bytes = new byte[1024];
                int read;
                StringBuilder builder = new StringBuilder();
                while ((read = in.read(bytes)) != -1) builder.append(new String(bytes, 0, read));
                con.disconnect();

                JsonObject object = JsonParser.parseString(builder.toString()).getAsJsonObject();
                String latestVersion = object.get("tag_name").getAsString().substring(1);
                if (!VERSION.equals(latestVersion)) {
                    Logger.LOGGER.warn("You are running an outdated version of ViaProxy! Latest version: " + latestVersion);
                    if (hasUI) {
                        SwingUtilities.invokeLater(() -> {
                            JFrame frontFrame = new JFrame();
                            frontFrame.setAlwaysOnTop(true);
                            JOptionPane.showMessageDialog(frontFrame, "You are running an outdated version of ViaProxy!\nCurrent version: " + VERSION + "\nLatest version: " + latestVersion, "ViaProxy", JOptionPane.WARNING_MESSAGE);
                        });
                    }
                }
            } catch (Throwable ignored) {
            }
        }, "UpdateCheck");

        if (hasUI) {
            loaderThread.start();
            accountRefreshThread.start();
            final ViaProxyUI[] ui = new ViaProxyUI[1];
            SwingUtilities.invokeLater(() -> ui[0] = new ViaProxyUI());
            updateCheckThread.start();
            loaderThread.join();
            accountRefreshThread.join();
            while (ui[0] == null) {
                Logger.LOGGER.info("Waiting for UI to be initialized...");
                Thread.sleep(1000);
            }
            ui[0].setReady();
            Logger.LOGGER.info("ViaProxy started successfully!");
            return;
        }

        try {
            Options.parse(args);
        } catch (Throwable t) {
            Logger.LOGGER.fatal("[" + t.getClass().getSimpleName() + "] " + t.getMessage());
            System.exit(0);
        }

        updateCheckThread.start();
        loaderThread.start();
        loaderThread.join();
        Logger.LOGGER.info("ViaProxy started successfully!");
        startProxy();
    }

    public static void startProxy() {
        if (currentProxyServer != null) {
            throw new IllegalStateException("Proxy is already running");
        }
        try {
            currentProxyServer = new NetServer(Client2ProxyHandler::new, Client2ProxyChannelInitializer::new);
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

    private static void setNettyParameters() {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
        if (System.getProperty("io.netty.allocator.maxOrder") == null) {
            System.setProperty("io.netty.allocator.maxOrder", "9");
        }
        if (Options.NETTY_THREADS > 0) {
            System.setProperty("io.netty.eventLoopThreads", Integer.toString(Options.NETTY_THREADS));
        }
    }

}
