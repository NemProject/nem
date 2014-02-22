package org.nem.core.serialization;

import org.nem.core.utils.StringEncoding;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;

/**
 * A binary deserializer that supports forward-only deserialization.
 */
public class BinaryDeserializer implements AutoCloseable, Deserializer {

    private final ByteArrayInputStream stream;

    /**
     * Creates a new binary deserializer.
     *
     * @param bytes The byte array from which to read.
     */
    public BinaryDeserializer(final byte[] bytes) {
        this.stream = new ByteArrayInputStream(bytes);
    }

    @Override
    public int readInt(final String label) {
        byte[] bytes = this.readBytes(4);
        return bytes[0] & 0x000000FF
            | (bytes[1] << 8) & 0x0000FF00
            | (bytes[2] << 16) & 0x00FF0000
            | (bytes[3] << 24) & 0xFF000000;
    }

    @Override
    public long readLong(final String label) {
        long lowPart = this.readInt(null);
        long highPart = this.readInt(null);
        return lowPart & 0x00000000FFFFFFFFL
            | (highPart << 32) & 0xFFFFFFFF00000000L;
    }

    @Override
    public BigInteger readBigInteger(final String label) {
        byte[] bytes = this.readBytes(null);
        return new BigInteger(bytes);
    }

    @Override
    public byte[] readBytes(final String label) {
        int numBytes = this.readInt(null);
        return this.readBytes(numBytes);
    }

    @Override
    public String readString(final String label) {
        byte[] bytes = this.readBytes(null);
        return StringEncoding.getString(bytes);
    }

    @Override
    public void close() throws Exception {
        this.stream.close();
    }

    /**
     * Determines if there is more data left to read.
     *
     * @return true if there is more data left to read.
     */
    public Boolean hasMoreData() {
        return 0 != this.stream.available();
    }

    private byte[] readBytes(int numBytes) {
        if (this.stream.available() < numBytes)
            throw new SerializationException("unexpected end of stream reached");

        try {
            byte[] bytes = new byte[numBytes];
            int numBytesRead = this.stream.read(bytes);
            if (numBytesRead != numBytes)
                throw new SerializationException("unexpected end of stream reached");

            return bytes;
        }
        catch (IOException e) {
            throw new SerializationException(e);
        }
    }
}
