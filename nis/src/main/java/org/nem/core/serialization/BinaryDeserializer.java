package org.nem.core.serialization;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;

public class BinaryDeserializer implements AutoCloseable, Deserializer {

    private final ByteArrayInputStream stream;

    public BinaryDeserializer(final byte[] bytes) throws Exception {
        this.stream = new ByteArrayInputStream(bytes);
    }

    @Override
    public int readInt(final String label) throws Exception {
        byte[] bytes = this.readBytes(4);
        return bytes[0] & 0x000000FF
            | (bytes[1] << 8) & 0x0000FF00
            | (bytes[2] << 16) & 0x00FF0000
            | (bytes[3] << 24) & 0xFF000000;
    }

    @Override
    public long readLong(final String label) throws Exception {
        long lowPart = this.readInt(label);
        long highPart = this.readInt(label);
        return lowPart & 0x00000000FFFFFFFFL
            | (highPart << 32) & 0xFFFFFFFF00000000L;
    }

    public BigInteger readBigInteger(final String label) throws Exception {
        byte[] bytes = this.readBytes(label);
        return new BigInteger(bytes);
    }

    @Override
    public byte[] readBytes(final String label) throws Exception {
        int numBytes = this.readInt(label);
        return this.readBytes(numBytes);
    }

    @Override
    public String readString(final String label) throws Exception {
        byte[] bytes = this.readBytes(label);
        return new String(bytes, "UTF-8");
    }

    public Boolean hasMoreData() {
        return 0 != this.stream.available();
    }

    @Override
    public void close() throws Exception {
        this.stream.close();
    }

    private byte[] readBytes(int numBytes) throws Exception {
        if (this.stream.available() < numBytes)
            throw new SerializationException("unexpected end of stream reached");

        byte[] bytes = new byte[numBytes];
        int numBytesRead = this.stream.read(bytes);
        if (numBytesRead != numBytes)
            throw new SerializationException("unexpected end of stream reached");

        return bytes;
    }
}
