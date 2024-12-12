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
package net.raphimc.viaproxy.tasks;

import net.raphimc.viaproxy.util.logging.Logger;

import javax.swing.*;

public class SystemRequirementsCheck {

    public static void run(final boolean hasUI) {
        if ("32".equals(System.getProperty("sun.arch.data.model")) && Runtime.getRuntime().maxMemory() < 256 * 1024 * 1024) {
            Logger.LOGGER.fatal("ViaProxy is not able to run on 32-Bit Java. Please install 64-Bit Java.");
            if (hasUI) {
                JOptionPane.showMessageDialog(null, "ViaProxy is not able to run on 32-Bit Java. Please install 64-Bit Java.", "ViaProxy", JOptionPane.ERROR_MESSAGE);
            }
            System.exit(1);
        }

        if (Runtime.getRuntime().maxMemory() < 256 * 1024 * 1024) {
            Logger.LOGGER.fatal("ViaProxy is not able to run with less than 256MB of RAM.");
            if (hasUI) {
                JOptionPane.showMessageDialog(null, "ViaProxy is not able to run with less than 256MB of RAM.", "ViaProxy", JOptionPane.ERROR_MESSAGE);
            }
            System.exit(1);
        } else if (Runtime.getRuntime().maxMemory() < 512 * 1024 * 1024) {
            Logger.LOGGER.warn("ViaProxy has less than 512MB of RAM. This may cause issues with multiple clients connected.");
            if (hasUI) {
                JOptionPane.showMessageDialog(null, "ViaProxy has less than 512MB of RAM. This may cause issues with multiple clients connected.", "ViaProxy", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

}
