/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
