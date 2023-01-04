package net.raphimc.viaproxy.cli;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.connection.UserConnectionImpl;
import net.raphimc.viaprotocolhack.commands.UserCommandSender;
import net.raphimc.viaproxy.plugins.PluginManager;
import net.raphimc.viaproxy.plugins.events.ConsoleCommandEvent;
import net.raphimc.viaproxy.util.ArrayHelper;

import java.util.Arrays;
import java.util.Scanner;

public class ConsoleHandler {

    public static void hookConsole() {
        new Thread(ConsoleHandler::listen, "Console-Handler").start();
    }

    private static void listen() {
        final Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            final String line = scanner.nextLine();
            final String[] parts = line.split(" ");
            if (parts.length == 0) continue;
            final String command = parts[0];
            final ArrayHelper args = new ArrayHelper(Arrays.copyOfRange(parts, 1, parts.length));

            if (command.equalsIgnoreCase("gc")) {
                System.gc();
                System.out.println("GC Done");
            } else if (command.equalsIgnoreCase("via")) {
                Via.getManager().getCommandHandler().onCommand(new UserCommandSender(new UserConnectionImpl(null, true)), args.getAsArray());
            } else {
                if (PluginManager.EVENT_MANAGER.call(new ConsoleCommandEvent(command, args.getAsArray())).isCancelled()) continue;
                System.out.println("Invalid Command!");
                System.out.println(" via | Run a viaversion command");
                System.out.println(" gc | Run the garbage collector");
            }
        }
    }

}
