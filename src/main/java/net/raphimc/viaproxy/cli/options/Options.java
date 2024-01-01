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
package net.raphimc.viaproxy.cli.options;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.raphimc.vialoader.util.VersionEnum;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.plugins.events.PostOptionsParseEvent;
import net.raphimc.viaproxy.plugins.events.PreOptionsParseEvent;
import net.raphimc.viaproxy.saves.impl.accounts.Account;
import net.raphimc.viaproxy.util.AddressUtil;
import net.raphimc.viaproxy.util.logging.Logger;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static java.util.Arrays.asList;

public class Options {

    public static SocketAddress BIND_ADDRESS;
    public static SocketAddress CONNECT_ADDRESS;
    public static VersionEnum PROTOCOL_VERSION;
    public static boolean ONLINE_MODE;
    public static boolean OPENAUTHMOD_AUTH;
    public static boolean BETACRAFT_AUTH;
    public static Account MC_ACCOUNT;
    public static URI PROXY_URL; // Example: type://address:port or type://username:password@address:port
    public static boolean IGNORE_PACKET_TRANSLATION_ERRORS;

    // GUI only config options
    public static String CLASSIC_MP_PASS;
    public static Boolean LEGACY_SKIN_LOADING;
    public static boolean CHAT_SIGNING;

    // CLI only config options
    public static int COMPRESSION_THRESHOLD = 256;
    public static boolean SRV_MODE; // Example: lenni0451.net_25565_1.8.x.viaproxy.127.0.0.1.nip.io
    public static boolean INTERNAL_SRV_MODE; // Example: ip:port\7version\7mppass
    public static String RESOURCE_PACK_URL; // Example: http://example.com/resourcepack.zip
    public static boolean SERVER_HAPROXY_PROTOCOL;
    public static boolean LEGACY_CLIENT_PASSTHROUGH;

    public static void parse(final String[] args) throws IOException {
        final OptionParser parser = new OptionParser();
        final OptionSpec<Void> help = parser.acceptsAll(asList("help", "h", "?"), "Get a list of all arguments").forHelp();

        final OptionSpec<String> bindAddress = parser.acceptsAll(asList("bind_address", "bind_ip", "ba"), "The address the proxy should bind to").withRequiredArg().ofType(String.class).defaultsTo("0.0.0.0:25568");
        final OptionSpec<String> connectAddress = parser.acceptsAll(asList("connect_address", "target_ip", "ca", "a"), "The address of the target server").withRequiredArg().ofType(String.class).required();
        final OptionSpec<VersionEnum> version = parser.acceptsAll(asList("version", "v"), "The version of the target server").withRequiredArg().withValuesConvertedBy(new VersionEnumConverter()).required();
        final OptionSpec<Void> srvMode = parser.acceptsAll(asList("srv_mode", "srv", "s"), "Enable srv mode");
        final OptionSpec<Void> iSrvMode = parser.acceptsAll(asList("internal_srv_mode", "isrv"), "Enable internal srv mode").availableUnless(srvMode);
        final OptionSpec<Void> proxyOnlineMode = parser.acceptsAll(asList("online_mode", "om", "o"), "Enable proxy online mode");
        final OptionSpec<Integer> compressionThreshold = parser.acceptsAll(asList("compression_threshold", "ct", "c"), "The threshold for packet compression").withRequiredArg().ofType(Integer.class).defaultsTo(COMPRESSION_THRESHOLD);
        final OptionSpec<Void> openAuthModAuth = parser.acceptsAll(asList("openauthmod_auth", "oam_auth"), "Use OpenAuthMod for joining online mode servers");
        final OptionSpec<Integer> guiAccountIndex = parser.acceptsAll(asList("gui_account_index", "gui_account"), "Use an account from the ViaProxy GUI for joining online mode servers (Specify -1 for instructions)").withRequiredArg().ofType(Integer.class);
        final OptionSpec<Void> betaCraftAuth = parser.accepts("betacraft_auth", "Use BetaCraft authentication for classic servers");
        final OptionSpec<String> resourcePackUrl = parser.acceptsAll(asList("resource_pack_url", "resource_pack", "rpu", "rp"), "URL of a resource pack which clients can optionally download").withRequiredArg().ofType(String.class);
        final OptionSpec<String> proxyUrl = parser.acceptsAll(asList("proxy_url", "proxy"), "URL of a SOCKS(4/5)/HTTP(S) proxy which will be used for backend TCP connections").withRequiredArg().ofType(String.class);
        final OptionSpec<Void> serverHaProxyProtocol = parser.acceptsAll(asList("server-haproxy-protocol", "server-haproxy"), "Send HAProxy protocol messages to the backend server");
        final OptionSpec<Void> legacyClientPassthrough = parser.acceptsAll(asList("legacy_client_passthrough", "legacy_passthrough"), "Allow <= 1.6.4 clients to connect to the backend server (No protocol translation)");
        final OptionSpec<Void> ignorePacketTranslationErrors = parser.acceptsAll(List.of("ignore-packet-translation-errors"), "Enabling this will prevent getting disconnected from the server when a packet translation error occurs and instead only print the error in the console. This may cause issues depending on the type of packet which failed to translate");
        ViaProxy.EVENT_MANAGER.call(new PreOptionsParseEvent(parser));

        final OptionSet options;
        try {
            options = parser.parse(args);

            if (options.has(help)) {
                parser.formatHelpWith(new BetterHelpFormatter());
                parser.printHelpOn(Logger.SYSOUT);
                System.exit(1);
            }

            PROTOCOL_VERSION = options.valueOf(version);
            BIND_ADDRESS = AddressUtil.parse(options.valueOf(bindAddress), null);
            CONNECT_ADDRESS = AddressUtil.parse(options.valueOf(connectAddress), PROTOCOL_VERSION);
            SRV_MODE = options.has(srvMode);
            INTERNAL_SRV_MODE = options.has(iSrvMode);
            ONLINE_MODE = options.has(proxyOnlineMode);
            COMPRESSION_THRESHOLD = options.valueOf(compressionThreshold);
            OPENAUTHMOD_AUTH = options.has(openAuthModAuth);
            if (options.has(guiAccountIndex)) {
                final List<Account> accounts = ViaProxy.getSaveManager().accountsSave.getAccounts();
                final int index = options.valueOf(guiAccountIndex);
                if (index < 0 || index >= accounts.size()) {
                    Logger.LOGGER.error("Invalid account index: " + index);
                    Logger.LOGGER.info("To use this feature you have to:");
                    Logger.LOGGER.info("1. Launch the ViaProxy GUI on your computer and add an account in the GUI");
                    Logger.LOGGER.info("2. Copy the 'saves.json' file from the directory of your ViaProxy GUI jar to the directory of the current ViaProxy CLI jar");
                    Logger.LOGGER.info("3. Specify the index of the account (See list below)");
                    Logger.LOGGER.info("=== Account list ===");
                    for (int i = 0; i < accounts.size(); i++) {
                        Logger.LOGGER.info(i + ": " + accounts.get(i).getDisplayString());
                    }
                    Logger.LOGGER.info("====================");
                    System.exit(1);
                } else {
                    MC_ACCOUNT = accounts.get(index);
                    Logger.LOGGER.info("Using account: " + MC_ACCOUNT.getDisplayString() + " to join online mode servers");
                }
            }
            BETACRAFT_AUTH = options.has(betaCraftAuth);
            if (options.has(resourcePackUrl)) {
                RESOURCE_PACK_URL = options.valueOf(resourcePackUrl);
            }
            if (options.has(proxyUrl)) {
                try {
                    PROXY_URL = new URI(options.valueOf(proxyUrl));
                } catch (URISyntaxException e) {
                    Logger.LOGGER.error("Invalid proxy url: " + options.valueOf(proxyUrl));
                    Logger.LOGGER.error("Proxy url format: type://address:port or type://username:password@address:port");
                    System.exit(1);
                }
            }
            SERVER_HAPROXY_PROTOCOL = options.has(serverHaProxyProtocol);
            LEGACY_CLIENT_PASSTHROUGH = options.has(legacyClientPassthrough);
            IGNORE_PACKET_TRANSLATION_ERRORS = options.has(ignorePacketTranslationErrors);
            ViaProxy.EVENT_MANAGER.call(new PostOptionsParseEvent(options));
        } catch (OptionException e) {
            Logger.LOGGER.error("Error parsing options: " + e.getMessage());
            parser.formatHelpWith(new BetterHelpFormatter());
            parser.printHelpOn(Logger.SYSOUT);
            System.exit(1);
        }
    }

}
