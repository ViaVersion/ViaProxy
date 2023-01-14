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
package net.raphimc.viaproxy.protocolhack.viaproxy.signature.model;

import net.lenni0451.mcstructs.text.ATextComponent;
import net.lenni0451.mcstructs.text.components.StringComponent;

public class DecoratableMessage {

    private final String plain;
    private final ATextComponent decorated;

    public DecoratableMessage(final String plain) {
        this(plain, new StringComponent(plain));
    }

    public DecoratableMessage(final String plain, final ATextComponent decorated) {
        this.plain = plain;
        this.decorated = decorated;
    }

    public boolean isDecorated() {
        return !this.decorated.equals(new StringComponent(this.plain));
    }

    public String getPlain() {
        return this.plain;
    }

    public ATextComponent getDecorated() {
        return this.decorated;
    }

}
