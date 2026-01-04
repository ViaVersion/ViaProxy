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
package net.raphimc.viaproxy.cli;

import net.raphimc.viaproxy.cli.command.CommandManager;
import net.raphimc.viaproxy.util.logging.Logger;

import java.io.IOException;
import java.util.Scanner;

public class ConsoleHandler {

    public static void hookConsole() {
        try { // Best way I could find to check if a console is attached to System.in
            System.in.available();
        } catch (IOException e) {
            Logger.LOGGER.info("Console input is not available. CLI commands are disabled.");
            return;
        }

        new Thread(ConsoleHandler::listen, "Console-Handler").start();
    }

    private static void listen() {
        final CommandManager commandManager = new CommandManager();
        final Scanner scanner = new Scanner(System.in);
        try {
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();
                try {
                    commandManager.execute(line);
                } catch (Throwable e) {
                    Logger.LOGGER.error("Error while handling console input", e);
                }
            }
        } catch (Throwable e) {
            Logger.LOGGER.error("Error while reading console input", e);
        }
    }

}
