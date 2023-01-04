package net.raphimc.viaproxy.plugins.events.types;

public interface ICancellable {

    void setCancelled(final boolean cancelled);

    boolean isCancelled();

}
