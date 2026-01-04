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
package net.raphimc.viaproxy.cli.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.raphimc.viaproxy.cli.command.Command;
import net.raphimc.viaproxy.cli.command.executor.CommandExecutor;

public class ThreadDumpCommand extends Command {

    public ThreadDumpCommand() {
        super("threaddump", "Print the stacktrace of the currently running threads", "threaddump");
    }

    @Override
    public void register(final LiteralArgumentBuilder<CommandExecutor> builder) {
        builder.executes(context -> {
            StringBuilder dump = new StringBuilder();
            Thread.getAllStackTraces().forEach((thread, stackTrace) -> {
                dump.append(thread.getName()).append(":\n");
                for (StackTraceElement element : stackTrace) {
                    dump.append("\tat ").append(element).append("\n");
                }
            });
            context.getSource().sendMessage(dump.toString());
            return 1;
        });
    }

}
