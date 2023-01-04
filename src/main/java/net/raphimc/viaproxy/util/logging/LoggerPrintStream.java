package net.raphimc.viaproxy.util.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.OutputStream;
import java.io.PrintStream;

public class LoggerPrintStream extends PrintStream {

    protected static final Logger LOGGER = LogManager.getLogger();
    protected final String name;

    public LoggerPrintStream(final String name, final OutputStream out) {
        super(out);

        this.name = name;
    }

    public void println(final String message) {
        this.log(message);
    }

    public void println(final Object object) {
        this.log(String.valueOf(object));
    }

    protected void log(final String message) {
        LOGGER.info("[{}]: {}", this.name, message);
    }

}
