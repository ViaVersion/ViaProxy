package net.raphimc.viaproxy.protocolhack.providers;

import com.viaversion.viaversion.api.connection.UserConnection;
import net.raphimc.vialegacy.ViaLegacy;
import net.raphimc.vialegacy.protocols.alpha.protocola1_0_17_1_0_17_4toa1_0_16_2.storage.TimeLockStorage;
import net.raphimc.vialegacy.protocols.classic.protocola1_0_15toc0_28_30.providers.ClassicCustomCommandProvider;

import java.util.logging.Level;

public class ViaProxyClassicCustomCommandProvider extends ClassicCustomCommandProvider {

    @Override
    public boolean handleChatMessage(UserConnection user, String message) {
        try {
            if (message.startsWith("/")) {
                message = message.substring(1);
                final String[] args = message.split(" ");
                if (args.length <= 0) return super.handleChatMessage(user, message);
                if (args[0].equals("settime")) {
                    try {
                        if (args.length > 1) {
                            final long time = Long.parseLong(args[1]) % 24_000L;
                            user.get(TimeLockStorage.class).setTime(time);
                            this.sendFeedback(user, "§aTime has been set to §6" + time);
                        } else {
                            throw new RuntimeException("Invalid usage");
                        }
                    } catch (Throwable ignored) {
                        this.sendFeedback(user, "§cUsage: /settime <Time (Long)>");
                    }
                    return true;
                }
            }
        } catch (Throwable e) {
            ViaLegacy.getPlatform().getLogger().log(Level.WARNING, "Error handling custom classic command", e);
        }

        return super.handleChatMessage(user, message);
    }

}
