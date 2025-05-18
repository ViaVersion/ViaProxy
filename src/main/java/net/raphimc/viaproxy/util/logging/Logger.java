/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2025 RK_01/RaphiMC and contributors
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
package net.raphimc.viaproxy.util.logging;

import com.mojang.authlib.GameProfile;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;
import net.raphimc.viaproxy.util.AddressUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.jline.jansi.AnsiConsole;

import java.io.PrintStream;
import java.net.SocketAddress;
import java.util.Locale;

public class Logger {

    static {
        PluginManager.addPackage("net.raphimc.viaproxy.util.logging");
    }

    public static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger("ViaProxy");

    public static final PrintStream SYSOUT = System.out;
    public static final PrintStream SYSERR = System.err;

    public static void setup() {
        if (System.console() != null) { // jANSI is the best lib. If there is no console it just segfaults the JVM process. Thanks!
            AnsiConsole.systemInstall();
        }
        System.setErr(new LoggerPrintStream("STDERR", SYSERR));
        System.setOut(new LoggerPrintStream("STDOUT", SYSOUT));
    }

    public static void u_info(final String title, final ProxyConnection proxyConnection, final String msg) {
        u_log(Level.INFO, title, proxyConnection, msg);
    }

    public static void u_warn(final String title, final ProxyConnection proxyConnection, final String msg) {
        u_log(Level.WARN, title, proxyConnection, msg);
    }

    public static void u_err(final String title, final ProxyConnection proxyConnection, final String msg) {
        u_log(Level.INFO, title, proxyConnection, msg);
    }

    public static void u_log(final Level level, final String title, final ProxyConnection proxyConnection, final String msg) {
        final SocketAddress address = proxyConnection.getC2P().remoteAddress();
        final GameProfile gameProfile = proxyConnection.getGameProfile();
        u_log(level, title, address, gameProfile, msg);
    }

    public static void u_log(final Level level, final String title, final SocketAddress address, final GameProfile gameProfile, final String msg) {
        LOGGER.log(level, "[" + title.toUpperCase(Locale.ROOT) + "] (" + AddressUtil.toString(address) + " | " + (gameProfile != null ? gameProfile.getName() : "null") + ") " + msg);
    }

}
