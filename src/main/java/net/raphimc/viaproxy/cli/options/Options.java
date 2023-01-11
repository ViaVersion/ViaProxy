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
package net.raphimc.viaproxy.cli.options;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.raphimc.mcauth.step.java.StepMCProfile;
import net.raphimc.viaprotocolhack.util.VersionEnum;
import net.raphimc.viaproxy.util.logging.Logger;

import java.io.IOException;

import static java.util.Arrays.asList;

public class Options {

    public static String BIND_ADDRESS = "0.0.0.0";
    public static int BIND_PORT = 25568;

    public static boolean SRV_MODE; // Example: lenni0451.net_25565_1.8.x.viaproxy.127.0.0.1.nip.io
    public static boolean INTERNAL_SRV_MODE; // Example: ip\7port\7version\7mppass
    public static boolean ONLINE_MODE;
    public static int NETTY_THREADS = 0;
    public static int COMPRESSION_THRESHOLD = 256;

    public static String CONNECT_ADDRESS;
    public static int CONNECT_PORT;
    public static VersionEnum PROTOCOL_VERSION;

    public static boolean OPENAUTHMOD_AUTH;
    public static boolean LOCAL_SOCKET_AUTH;
    public static boolean BETACRAFT_AUTH;

    // GUI only config options
    public static StepMCProfile.MCProfile MC_ACCOUNT;

    public static void parse(final String[] args) throws IOException {
        final OptionParser parser = new OptionParser();
        final OptionSpec<Void> help = parser.acceptsAll(asList("help", "h", "?"), "Get a list of all arguments").forHelp();

        final OptionSpec<String> bindAddress = parser.acceptsAll(asList("bind_address", "bind_ip", "ba"), "The address the proxy should bind to").withRequiredArg().ofType(String.class).defaultsTo(BIND_ADDRESS);
        final OptionSpec<Integer> bindPort = parser.acceptsAll(asList("bind_port", "bp"), "The port the proxy should bind to").withRequiredArg().ofType(Integer.class).defaultsTo(BIND_PORT);
        final OptionSpec<Void> srvMode = parser.acceptsAll(asList("srv_mode", "srv", "s"), "Enable srv mode");
        final OptionSpec<Void> iSrvMode = parser.acceptsAll(asList("internal_srv_mode", "isrv"), "Enable internal srv mode").availableUnless(srvMode);
        final OptionSpec<Void> onlineMode = parser.acceptsAll(asList("online_mode", "om", "o"), "Enable online mode");
        final OptionSpec<Integer> nettyThreads = parser.acceptsAll(asList("netty_threads", "t"), "The amount of netty threads to use").withRequiredArg().ofType(Integer.class).defaultsTo(NETTY_THREADS);
        final OptionSpec<Integer> compressionThreshold = parser.acceptsAll(asList("compression_threshold", "ct", "c"), "The threshold for packet compression").withRequiredArg().ofType(Integer.class).defaultsTo(COMPRESSION_THRESHOLD);
        final OptionSpec<String> connectAddress = parser.acceptsAll(asList("connect_address", "target_ip", "ca", "a"), "The address of the target server").withRequiredArg().ofType(String.class).required();
        final OptionSpec<Integer> connectPort = parser.acceptsAll(asList("connect_port", "target_port", "cp", "p"), "The port of the target server").withRequiredArg().ofType(Integer.class).defaultsTo(CONNECT_PORT);
        final OptionSpec<VersionEnum> version = parser.acceptsAll(asList("version", "v"), "The version of the target server").withRequiredArg().withValuesConvertedBy(new VersionEnumConverter()).required();
        final OptionSpec<Void> openAuthModAuth = parser.acceptsAll(asList("openauthmod_auth", "oam_auth"), "Enable OpenAuthMod authentication");
        final OptionSpec<Void> localSocketAuth = parser.accepts("local_socket_auth", "Enable authentication over a local socket");
        final OptionSpec<Void> betaCraftAuth = parser.accepts("betacraft_auth", "Use BetaCraft authentication servers for classic");

        final OptionSet options = parser.parse(args);
        if (options.has(help)) {
            parser.formatHelpWith(new BetterHelpFormatter());
            parser.printHelpOn(Logger.SYSOUT);
            System.exit(0);
        }

        BIND_ADDRESS = options.valueOf(bindAddress);
        BIND_PORT = options.valueOf(bindPort);
        SRV_MODE = options.has(srvMode);
        INTERNAL_SRV_MODE = options.has(iSrvMode);
        ONLINE_MODE = options.has(onlineMode);
        NETTY_THREADS = options.valueOf(nettyThreads);
        CONNECT_ADDRESS = options.valueOf(connectAddress);
        CONNECT_PORT = options.valueOf(connectPort);
        PROTOCOL_VERSION = options.valueOf(version);
        COMPRESSION_THRESHOLD = options.valueOf(compressionThreshold);
        OPENAUTHMOD_AUTH = options.has(openAuthModAuth);
        LOCAL_SOCKET_AUTH = options.has(localSocketAuth);
        BETACRAFT_AUTH = options.has(betaCraftAuth);
    }

}
