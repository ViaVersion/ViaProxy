package net.raphimc.viaproxy.util.logging;

import com.mojang.authlib.GameProfile;
import com.viaversion.viaversion.api.connection.UserConnection;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Locale;

public class Logger {

    public static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger("ViaProxy");

    public static final PrintStream SYSOUT = System.out;
    public static final PrintStream SYSERR = System.err;

    public static void setup() {
        System.setErr(new LoggerPrintStream("STDERR", SYSERR));
        System.setOut(new LoggerPrintStream("STDOUT", SYSOUT));
    }

    public static void u_info(final String title, final SocketAddress address, final GameProfile gameProfile, final String msg) {
        u_log(Level.INFO, title, address, gameProfile, msg);
    }

    public static void u_err(final String title, final SocketAddress address, final GameProfile gameProfile, final String msg) {
        u_log(Level.ERROR, title, address, gameProfile, msg);
    }

    public static void u_err(final String title, final UserConnection user, final String msg) {
        GameProfile gameProfile = null;
        if (user.getProtocolInfo().getUsername() != null) {
            gameProfile = new GameProfile(user.getProtocolInfo().getUuid(), user.getProtocolInfo().getUsername());
        }
        u_log(Level.ERROR, title, user.getChannel().remoteAddress(), gameProfile, msg);
    }

    public static void u_log(final Level level, final String title, final SocketAddress address, final GameProfile gameProfile, final String msg) {
        final InetSocketAddress socketAddress = (InetSocketAddress) address;
        LOGGER.log(level, "[" + title.toUpperCase(Locale.ROOT) + "] (" + socketAddress.getAddress().getHostAddress() + " | " + (gameProfile != null ? gameProfile.getName() : "null") + ") " + msg);
    }

}
