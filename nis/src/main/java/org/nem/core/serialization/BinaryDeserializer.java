package org.nem.core.serialization;

import org.nem.core.utils.StringEncoder;

import java.io.*;
import java.math.BigInteger;
import java.util.*;

/**
 * A binary deserializer that supports forward-only deserialization.
 */
public class BinaryDeserializer implements AutoCloseable, Deserializer {

    private final ByteArrayInputStream stream;
    private final DeserializationContext context;

    /**
     * Creates a new binary deserializer.
     *
     * @param bytes The byte array from which to read.
     * @param  context The deserialization context.
     */
    public BinaryDeserializer(final byte[] bytes, final DeserializationContext context) {
        this.stream = new ByteArrayInputStream(bytes);
        this.context = context;
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
        return StringEncoder.getString(bytes);
    }

    @Override
    public <T> T readObject(final String label, final ObjectDeserializer<T> activator) {
        return this.deserializeObject(activator);
    }

    @Override
    public <T> List<T> readObjectArray(final String label, final ObjectDeserializer<T> activator) {
        List<T> objects = new ArrayList<>();
        int numObjects = this.readInt(null);
        for (int i = 0; i < numObjects; ++i)
            objects.add(deserializeObject(activator));

        return objects;
    }

    @Override
    public DeserializationContext getContext() {
        return this.context;
    }

    @Override
    public void close() throws Exception {
        this.stream.close();
    }

    private <T> T deserializeObject(final ObjectDeserializer<T> activator) {
        try {
            byte[] bytes = this.readBytes(null);
            try (BinaryDeserializer deserializer = new BinaryDeserializer(bytes, this.getContext())) {
                return activator.deserialize(deserializer);
            }
        }
        catch (Exception ex) {
            throw new SerializationException(ex);
        }
    }

    /**
     * Determines if there is more data left to read.
     *
     * @return true if there is more data left to read.
     */
    public boolean hasMoreData() {
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
