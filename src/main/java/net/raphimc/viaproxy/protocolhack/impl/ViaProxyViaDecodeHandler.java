package net.raphimc.viaproxy.protocolhack.impl;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.exception.CancelCodecException;
import com.viaversion.viaversion.util.PipelineUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.raphimc.viaprotocolhack.netty.ViaDecodeHandler;
import net.raphimc.viaproxy.util.logging.Logger;

import java.util.List;

public class ViaProxyViaDecodeHandler extends ViaDecodeHandler {

    public ViaProxyViaDecodeHandler(final UserConnection info) {
        super(info);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf bytebuf, List<Object> out) throws Exception {
        try {
            super.decode(ctx, bytebuf, out);
        } catch (Throwable e) {
            if (PipelineUtil.containsCause(e, CancelCodecException.class)) throw e;
            Logger.LOGGER.error("ProtocolHack Packet Error occurred", e);
            Logger.u_err("ProtocolHack Error", this.user, "Caught unhandled exception: " + e.getClass().getSimpleName());
        }
    }

}
