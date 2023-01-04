package net.raphimc.viaproxy.plugins.events;

import io.netty.channel.socket.SocketChannel;
import net.raphimc.viaproxy.plugins.events.types.ICancellable;
import net.raphimc.viaproxy.plugins.events.types.ITyped;

public class Proxy2ServerChannelInitializeEvent implements ICancellable, ITyped {

    private final Type type;
    private final SocketChannel channel;

    private boolean cancelled;

    public Proxy2ServerChannelInitializeEvent(final Type type, final SocketChannel channel) {
        this.type = type;
        this.channel = channel;
    }

    public SocketChannel getChannel() {
        return this.channel;
    }


    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public Type getType() {
        return this.type;
    }

}
