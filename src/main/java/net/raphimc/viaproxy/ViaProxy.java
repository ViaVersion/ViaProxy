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
import net.lenni0451.reflect.ClassLoaders;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.connection.NetServer;
import net.raphimc.vialegacy.util.VersionEnum;
import net.raphimc.viaproxy.cli.ConsoleHandler;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.plugins.PluginManager;
import net.raphimc.viaproxy.protocolhack.ProtocolHack;
import net.raphimc.viaproxy.proxy.ProxyConnection;
import net.raphimc.viaproxy.proxy.client2proxy.Client2ProxyChannelInitializer;
import net.raphimc.viaproxy.proxy.client2proxy.Client2ProxyHandler;
import net.raphimc.viaproxy.ui.ViaProxyUI;
import net.raphimc.viaproxy.util.logging.Logger;

import javax.swing.*;
import java.awt.*;

public class ViaProxy {

    public static final String VERSION = "${version}";

    public static NetServer currentProxyServer;
    public static Thread loaderThread;
    public static ChannelGroup c2pChannels;

    public static void main(String[] args) throws Throwable {
        final TransformerManager transformerManager = new TransformerManager(new GuavaClassPathProvider());
        transformerManager.addTransformerPreprocessor(new MixinsTranslator());
        transformerManager.addTransformer("net.raphimc.viaproxy.injection.transformer.**");
        transformerManager.addTransformer("net.raphimc.viaproxy.injection.mixins.**");
        final InjectionClassLoader injectionClassLoader = new InjectionClassLoader(transformerManager, ClassLoaders.getSystemClassPath());
        injectionClassLoader.setPriority(EnumLoaderPriority.PARENT_FIRST);
        injectionClassLoader.executeMain(ViaProxy.class.getName(), "injectedMain", args);
    }

    public static void injectedMain(String[] args) throws InterruptedException {
        Logger.setup();
        ConsoleHandler.hookConsole();
        Logger.LOGGER.info("Initializing ViaProxy v" + VERSION + "...");
        VersionEnum.init();
        setNettyParameters();
        MCPipeline.useOptimizedPipeline();
        c2pChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        loaderThread = new Thread(() -> {
            ProtocolHack.init();
            PluginManager.loadPlugins();
        }, "ViaProtocolHack-Loader");

        if (args.length == 0 && !GraphicsEnvironment.isHeadless()) {
            loaderThread.start();
            final ViaProxyUI[] ui = new ViaProxyUI[1];
            SwingUtilities.invokeLater(() -> ui[0] = new ViaProxyUI());
            loaderThread.join();
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

        loaderThread.start();
        loaderThread.join();
        Logger.LOGGER.info("ViaProxy started successfully!");
        startProxy();
    }

    public static void startProxy() {
        if (currentProxyServer != null) {
            throw new IllegalStateException("Proxy is already running");
        }
        currentProxyServer = new NetServer(Client2ProxyHandler::new, Client2ProxyChannelInitializer::new);
        Logger.LOGGER.info("Binding proxy server to " + Options.BIND_ADDRESS + ":" + Options.BIND_PORT);
        currentProxyServer.bind(Options.BIND_ADDRESS, Options.BIND_PORT, false);
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
