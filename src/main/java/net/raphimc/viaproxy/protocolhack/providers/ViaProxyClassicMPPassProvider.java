package net.raphimc.viaproxy.protocolhack.providers;

import com.google.common.hash.Hashing;
import com.google.common.io.Resources;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import net.raphimc.vialegacy.protocols.classic.protocola1_0_15toc0_28_30.providers.ClassicMPPassProvider;
import net.raphimc.vialegacy.protocols.release.protocol1_3_1_2to1_2_4_5.providers.OldAuthProvider;
import net.raphimc.vialegacy.protocols.release.protocol1_7_2_5to1_6_4.storage.HandshakeStorage;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.proxy.ProxyConnection;

import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class ViaProxyClassicMPPassProvider extends ClassicMPPassProvider {

    @Override
    public String getMpPass(UserConnection user) {
        final String mppass = ProxyConnection.fromUserConnection(user).getClassicMpPass();
        if (mppass != null && !mppass.isEmpty() && !mppass.equals("0")) {
            return mppass;
        } else if (Options.BETACRAFT_AUTH) {
            final HandshakeStorage handshakeStorage = user.get(HandshakeStorage.class);
            return getBetacraftMpPass(user, user.getProtocolInfo().getUsername(), handshakeStorage.getHostname(), handshakeStorage.getPort());
        } else {
            return super.getMpPass(user);
        }
    }

    private static String getBetacraftMpPass(final UserConnection user, final String username, final String serverIp, final int port) {
        try {
            final String server = InetAddress.getByName(serverIp).getHostAddress() + ":" + port;
            Via.getManager().getProviders().get(OldAuthProvider.class).sendAuthRequest(user, Hashing.sha1().hashBytes(server.getBytes()).toString());
            final String mppass = Resources.toString(new URL("http://api.betacraft.uk/getmppass.jsp?user=" + username + "&server=" + server), StandardCharsets.UTF_8);
            if (mppass.contains("FAILED") || mppass.contains("SERVER NOT FOUND")) return "0";
            return mppass;
        } catch (Throwable e) {
            Via.getPlatform().getLogger().log(Level.WARNING, "An unknown error occurred while authenticating with BetaCraft", e);
        }
        return "0";
    }

}
