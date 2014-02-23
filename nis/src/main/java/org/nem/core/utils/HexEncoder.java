package org.nem.core.utils;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

/**
 * Static class that contains utility functions for converting hex strings to and from bytes.
 */
public class HexEncoder {
    /**
     * Converts a string to a byte array.
     *
     * @param hexString The input hex string.
     * @return The output byte array.
     */
    public static byte[] getBytes(final String hexString) throws DecoderException {
        Hex codec = new Hex();
        byte[] encodedBytes = StringEncoder.getBytes(hexString);
        return codec.decode(encodedBytes);
    }

    /**
     * Converts a byte array to a hex string.
     *
     * @param bytes The input byte array.
     * @return The output hex string.
     */
    public static String getString(byte[] bytes) {
        Hex codec = new Hex();
        byte[] decodedBytes = codec.encode(bytes);
        return StringEncoder.getString(decodedBytes);
    }
}
