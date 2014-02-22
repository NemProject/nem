package org.nem.core.serialization;

import org.nem.core.utils.StringEncoding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
/**
 * A binary serializer that supports forward-only serialization.
 */
public class BinarySerializer implements AutoCloseable, Serializer {

    private final ByteArrayOutputStream stream;

    /**
     * Creates a new binary serializer.
     */
    public BinarySerializer() {
        this.stream = new ByteArrayOutputStream();
    }

    @Override
    public void writeInt(final String label, final int i) {
        byte[] bytes = {
            (byte)(i & 0xFF),
            (byte)((i >> 8) & 0xFF),
            (byte)((i >> 16) & 0xFF),
            (byte)((i >> 24) & 0xFF),
        };
        this.writeBytesInternal(bytes);
    }

    @Override
    public void writeLong(final String label, final long l) {
        this.writeInt(null, (int)l);
        this.writeInt(null, (int)(l >> 32));
    }

    @Override
    public void writeBigInteger(final String label, final BigInteger i) {
        this.writeBytes(null, i.toByteArray());
    }

    @Override
    public void writeBytes(final String label, final byte[] bytes) {
        this.writeInt(null, bytes.length);
        this.writeBytesInternal(bytes);
    }

    @Override
    public void writeString(final String label, final String s) {
        this.writeBytes(null, StringEncoding.getBytes(s));
    }

    @Override
    public void close() throws IOException {
        this.stream.close();
    }

    /**
     * Gets the underlying byte buffer.
     *
     * @return The underlying byte buffer.
     */
    public byte[] getBytes() {
        return this.stream.toByteArray();
    }

    private void writeBytesInternal(final byte[] bytes) {
        this.stream.write(bytes, 0, bytes.length);
    }
}
