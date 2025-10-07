/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2025 RK_01/RaphiMC and contributors
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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.lenni0451.commons.brigadier.CommandBuilder;
import net.lenni0451.reflect.stream.RStream;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.cli.command.executor.CommandExecutor;
import net.raphimc.viaproxy.cli.command.executor.ConsoleCommandExecutor;
import net.raphimc.viaproxy.cli.command.impl.*;
import net.raphimc.viaproxy.plugins.events.ConsoleCommandEvent;
import net.raphimc.viaproxy.util.logging.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandManager implements CommandBuilder<CommandExecutor> {

    private final CommandDispatcher<CommandExecutor> dispatcher = new CommandDispatcher<>();
    private final List<Command> commands = new ArrayList<>();

    // Core commands
    private final HelpCommand HelpCommand = new HelpCommand(this);
    private final StopCommand StopCommand = new StopCommand();
    private final ViaVersionCommand ViaVersionCommand = new ViaVersionCommand();
    private final UploadLogCommand UploadLogCommand = new UploadLogCommand();
    private final AccountCommand AccountCommand = new AccountCommand();

    // Debugging commands
    private final GcCommand GcCommand = new GcCommand();
    private final ThreadDumpCommand ThreadDumpCommand = new ThreadDumpCommand();

    public CommandManager() {
        RStream
                .of(this)
                .fields()
                .filter(field -> Command.class.isAssignableFrom(field.type()))
                .forEach(field -> {
                    final Command command = field.get();
                    try {
                        for (String name : command.getNames()) {
                            final LiteralArgumentBuilder<CommandExecutor> builder = literal(name);
                            command.register(builder);
                            this.dispatcher.register(builder);
                        }
                        this.commands.add(command);
                    } catch (Throwable e) {
                        Logger.LOGGER.error("Failed to register command " + command.getNames()[0], e);
                    }
                });

        this.dispatcher.findAmbiguities((parent, child, sibling, inputs) -> Logger.LOGGER.warn("Ambiguity between arguments {} and {} with inputs: {}", this.dispatcher.getPath(child), this.dispatcher.getPath(sibling), inputs));
    }

    public void execute(final String message) {
        try {
            this.dispatcher.execute(message, ConsoleCommandExecutor.INSTANCE);
        } catch (CommandSyntaxException e) {
            if (e.getMessage().startsWith("Unknown command at position 0")) { // Compat for the old command API
                final String[] parts = message.split(" ");
                if (parts.length != 0) {
                    final String command = parts[0];
                    final String[] args = Arrays.copyOfRange(parts, 1, parts.length);
                    if (ViaProxy.EVENT_MANAGER.call(new ConsoleCommandEvent(command, args)).isCancelled()) {
                        return;
                    }
                }
                Logger.LOGGER.error(e.getMessage().replace("Unknown command", "Unknown or incomplete command"));
                Logger.LOGGER.error("Type 'help' for help and a list of available commands.");
            } else if (e.getMessage().startsWith("Unknown command at position")) {
                Logger.LOGGER.error(e.getMessage().replace("Unknown command", "Unknown or incomplete command"));
                Logger.LOGGER.error("Type 'help' for help and a list of available commands.");
            } else {
                Logger.LOGGER.error(e.getMessage());
            }
        }
    }

    public List<Command> getCommands() {
        return this.commands;
    }

}
