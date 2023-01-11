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
package net.raphimc.viaproxy.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class LocalSocketClient {

    private final int port;

    public LocalSocketClient(final int port) {
        this.port = port;
    }

    public String[] request(final String command, final String... args) {
        try {
            final Socket socket = new Socket();
            socket.setSoTimeout(500);
            socket.connect(new InetSocketAddress("127.0.0.1", this.port), 500);
            final DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            final DataInputStream in = new DataInputStream(socket.getInputStream());

            out.writeUTF(command);
            out.writeInt(args.length);
            for (String s : args) out.writeUTF(s);

            final String[] response = new String[in.readInt()];
            for (int i = 0; i < response.length; i++) response[i] = in.readUTF();
            socket.close();
            return response;
        } catch (Throwable e) {
            return null;
        }
    }

}
