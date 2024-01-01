/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2024 RK_01/RaphiMC and contributors
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

import io.netty.channel.Channel;
import net.raphimc.viaproxy.plugins.events.types.EventCancellable;
import net.raphimc.viaproxy.plugins.events.types.ITyped;

public class Client2ProxyChannelInitializeEvent extends EventCancellable implements ITyped {

    private final Type type;
    private final Channel channel;
    private final boolean isLegacyPassthrough;

    public Client2ProxyChannelInitializeEvent(final Type type, final Channel channel, final boolean isLegacyPassthrough) {
        this.type = type;
        this.channel = channel;
        this.isLegacyPassthrough = isLegacyPassthrough;
    }

    public Channel getChannel() {
        return this.channel;
    }

    public boolean isLegacyPassthrough() {
        return this.isLegacyPassthrough;
    }


    @Override
    public Type getType() {
        return this.type;
    }

}
