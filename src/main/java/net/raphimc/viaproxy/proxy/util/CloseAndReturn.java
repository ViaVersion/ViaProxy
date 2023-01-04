package net.raphimc.viaproxy.proxy.util;

public class CloseAndReturn extends RuntimeException {

    public static final CloseAndReturn INSTANCE = new CloseAndReturn();

    CloseAndReturn() {
    }

    @Override
    public String toString() {
        return "";
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

}
