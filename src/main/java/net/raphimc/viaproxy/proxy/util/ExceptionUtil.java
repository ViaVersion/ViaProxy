package net.raphimc.viaproxy.proxy.util;

import com.viaversion.viaversion.exception.InformativeException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import net.raphimc.viaproxy.proxy.ProxyConnection;
import net.raphimc.viaproxy.util.logging.Logger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.ClosedChannelException;
import java.util.Iterator;
import java.util.Map;

public class ExceptionUtil {

    private static Field infoField;

    static {
        try {
            infoField = InformativeException.class.getDeclaredField("info");
            infoField.setAccessible(true);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void handleNettyException(ChannelHandlerContext ctx, Throwable cause, ProxyConnection proxyConnection) {
        if (!ctx.channel().isOpen() || !ctx.channel().isActive()) return;
        if (cause instanceof ClosedChannelException) return;
        if (cause instanceof IOException) return;
        if (cause instanceof CloseAndReturn) {
            ctx.channel().close();
            return;
        }
        Logger.LOGGER.error("Caught unhandled netty exception", cause);
        try {
            proxyConnection.kickClient("§cAn unhandled error occurred in your connection and it has been closed.\n§aError details for report:§f" + ExceptionUtil.prettyPrint(cause));
        } catch (Throwable ignored) {
        }
        ctx.channel().close();
    }

    public static String prettyPrint(Throwable t) {
        StringBuilder msg = new StringBuilder();
        if (t instanceof EncoderException) t = t.getCause();
        while (t != null) {
            String exceptionMessage = t.getMessage();
            if (t instanceof InformativeException) {
                exceptionMessage = getMessageFor((InformativeException) t);
            }
            msg.append("\n");
            msg.append("§c").append(t.getClass().getSimpleName()).append("§7: §f").append(exceptionMessage);
            t = t.getCause();
            if (t != null) {
                msg.append(" §9Caused by");
            }
        }
        return msg.toString();
    }

    private static String getMessageFor(final InformativeException e) {
        Map<String, Object> info = null;
        try {
            info = (Map<String, Object>) infoField.get(e);
        } catch (Throwable ignored) {
        }
        if (info != null) {
            final StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (Iterator<Map.Entry<String, Object>> var3 = info.entrySet().iterator(); var3.hasNext(); first = false) {
                Map.Entry<String, Object> entry = var3.next();
                if (!first) {
                    builder.append(", ");
                }
                builder.append(entry.getKey()).append(": ").append(entry.getValue());
            }
            return builder.toString();
        }
        return "Unable to get source info";
    }

}
