package net.raphimc.viaproxy.protocolhack.providers;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.CompressionProvider;
import net.raphimc.netminecraft.constants.MCPipeline;

public class ViaProxyCompressionProvider extends CompressionProvider {

    @Override
    public void handlePlayCompression(UserConnection user, int threshold) {
        if (!user.isClientSide()) {
            throw new IllegalStateException("PLAY state Compression packet is unsupported");
        }
        user.getChannel().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).set(threshold);
    }

}
