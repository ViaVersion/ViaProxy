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

import java.io.IOException;
import java.io.InputStream;

public class DelayedStream extends InputStream {

    private final InputStream parent;
    private final long sleepDelay;

    public DelayedStream(final InputStream parent, final long sleepDelay) {
        this.parent = parent;
        this.sleepDelay = sleepDelay;
    }

    public InputStream getParent() {
        return this.parent;
    }

    public long getSleepDelay() {
        return this.sleepDelay;
    }

    public void waitForInput() throws IOException {
        while (this.available() <= 0) {
            try {
                Thread.sleep(this.sleepDelay);
            } catch (InterruptedException e) {
                throw new IOException("Interrupted", e);
            }
        }
    }

    @Override
    public int read() throws IOException {
        this.waitForInput();
        return this.parent.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        this.waitForInput();
        return this.parent.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        this.waitForInput();
        return this.parent.read(b, off, len);
    }

    @Override
    public int available() throws IOException {
        return this.parent.available();
    }

    @Override
    public void close() throws IOException {
        this.parent.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        this.parent.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        this.parent.reset();
    }

    @Override
    public boolean markSupported() {
        return this.parent.markSupported();
    }

}
