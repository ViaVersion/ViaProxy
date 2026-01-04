/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2026 RK_01/RaphiMC and contributors
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ProtocolTranslatorInitEvent {

    private final List<Supplier<?>> additionalPlatformSuppliers = new ArrayList<>();

    public ProtocolTranslatorInitEvent(final Supplier<?>... additionalPlatformSuppliers) {
        for (final Supplier<?> platformSupplier : additionalPlatformSuppliers) {
            this.registerPlatform(platformSupplier);
        }
    }

    public void registerPlatform(final Supplier<?> platformSupplier) {
        this.additionalPlatformSuppliers.add(platformSupplier);
    }

    public List<Supplier<?>> getPlatformSuppliers() {
        return this.additionalPlatformSuppliers;
    }

}
