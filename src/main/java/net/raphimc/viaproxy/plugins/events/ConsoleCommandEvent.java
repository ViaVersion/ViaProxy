package net.raphimc.viaproxy.plugins.events;

import net.raphimc.viaproxy.plugins.events.types.ICancellable;

public class ConsoleCommandEvent implements ICancellable {

    private final String command;
    private final String[] args;
    private boolean cancelled;

    public ConsoleCommandEvent(final String command, final String[] args) {
        this.command = command;
        this.args = args;
    }

    public String getCommand() {
        return this.command;
    }

    public String[] getArgs() {
        return this.args;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

}
