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
package net.raphimc.viaproxy.cli;

import com.viaversion.viaversion.api.Via;
import net.raphimc.viaproxy.plugins.PluginManager;
import net.raphimc.viaproxy.plugins.events.ConsoleCommandEvent;
import net.raphimc.viaproxy.protocolhack.viaproxy.ConsoleCommandSender;
import net.raphimc.viaproxy.util.ArrayHelper;
import net.raphimc.viaproxy.util.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

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
        final BufferedReader reader = new BufferedReader(new InputStreamReader(new DelayedStream(System.in, 500)));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    final String[] parts = line.split(" ");
                    if (parts.length == 0) continue;
                    final String command = parts[0];
                    final ArrayHelper args = new ArrayHelper(Arrays.copyOfRange(parts, 1, parts.length));

                    if (command.equalsIgnoreCase("gc")) {
                        System.gc();
                        System.out.println("GC Done");
                    } else if (command.equalsIgnoreCase("via")) {
                        Via.getManager().getCommandHandler().onCommand(new ConsoleCommandSender(), args.getAsArray());
                    } else if (command.equalsIgnoreCase("threaddump")) {
                        System.out.println("Thread Dump:");
                        for (Thread thread : Thread.getAllStackTraces().keySet()) {
                            System.out.println("Thread: " + thread.getName() + " | State: " + thread.getState());
                            for (StackTraceElement element : thread.getStackTrace()) System.out.println("    " + element.toString());
                        }
                    } else {
                        if (PluginManager.EVENT_MANAGER.call(new ConsoleCommandEvent(command, args.getAsArray())).isCancelled()) continue;
                        System.out.println("Invalid Command!");
                        System.out.println(" via | Run a viaversion command");
                        System.out.println(" gc | Run the garbage collector");
                        System.out.println(" threaddump | Print the stacktrace of all running threads");
                    }
                } catch (Throwable e) {
                    Logger.LOGGER.error("Error while handling console input", e);
                }
            }
        } catch (Throwable e) {
            Logger.LOGGER.error("Error while reading console input", e);
        }
    }

}
