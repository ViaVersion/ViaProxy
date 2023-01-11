package net.raphimc.viaproxy.proxy.external_interface;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class OpenAuthModConstants {

    public static final String BASE_CHANNEL = "oam:";
    public static final byte[] LEGACY_MAGIC_BYTES = new byte[]{2, 20, 12, 3}; // 1.8 - 1.12.2
    public static final String LEGACY_MAGIC_STRING = new String(LEGACY_MAGIC_BYTES, StandardCharsets.UTF_8); // 1.8 - 1.12.2
    public static final int LEGACY_MAGIC_INT = -new BigInteger(LEGACY_MAGIC_BYTES).intValueExact(); // 1.8 - 1.12.2

    // Request
    public static final String JOIN_CHANNEL = BASE_CHANNEL + "join"; // 1.8 - latest
    public static final String SIGN_NONCE_CHANNEL = BASE_CHANNEL + "sign_nonce"; // 1.19 - latest

    // Response
    public static final String DATA_CHANNEL = BASE_CHANNEL + "data"; // 1.8 - latest

}
