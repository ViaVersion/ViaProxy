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
package net.raphimc.viaproxy.cli.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.lenni0451.commons.brigadier.CommandBuilder;
import net.raphimc.viaproxy.cli.command.executor.CommandExecutor;

public abstract class Command implements CommandBuilder<CommandExecutor> {

    private final String[] names;
    private final String description;
    private final String help;

    public Command(final String name, final String description, final String help, final String... aliases) {
        this.names = new String[aliases.length + 1];
        this.names[0] = name;
        System.arraycopy(aliases, 0, this.names, 1, aliases.length);
        this.description = description;
        this.help = help;
    }

    public abstract void register(final LiteralArgumentBuilder<CommandExecutor> builder);

    public String getName() {
        return this.names[0];
    }

    public String[] getNames() {
        return this.names;
    }

    public String getDescription() {
        return this.description;
    }

    public String getHelp() {
        return this.help;
    }

}
