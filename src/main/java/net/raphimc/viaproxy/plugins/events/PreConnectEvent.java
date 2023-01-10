package net.raphimc.viaproxy.plugins.events;

import io.netty.channel.Channel;
import net.raphimc.netminecraft.util.ServerAddress;
import net.raphimc.viaprotocolhack.util.VersionEnum;
import net.raphimc.viaproxy.plugins.events.types.ICancellable;

public class PreConnectEvent implements ICancellable {

    private final ServerAddress serverAddress;
    private final VersionEnum serverVersion;
    private final VersionEnum clientVersion;
    private final Channel clientChannel;

    private boolean cancelled;
    private String cancelMessage = "§cCould not connect to the backend server! (Server is blacklisted)";

    public PreConnectEvent(final ServerAddress serverAddress, final VersionEnum serverVersion, final VersionEnum clientVersion, final Channel clientChannel) {
        this.serverAddress = serverAddress;
        this.serverVersion = serverVersion;
        this.clientVersion = clientVersion;
        this.clientChannel = clientChannel;
    }

    public ServerAddress getServerAddress() {
        return this.serverAddress;
    }

    public VersionEnum getServerVersion() {
        return this.serverVersion;
    }

    public VersionEnum getClientVersion() {
        return this.clientVersion;
    }

    public Channel getClientChannel() {
        return this.clientChannel;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    public String getCancelMessage() {
        return this.cancelMessage;
    }

    public void setCancelMessage(final String cancelMessage) {
        this.cancelMessage = cancelMessage;
    }

}
