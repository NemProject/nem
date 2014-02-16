package org.nem.core.serialization;

import org.nem.core.model.Account;

public interface Deserializer {
    public int readInt() throws Exception;
    public long readLong() throws Exception;
    public byte[] readBytes() throws Exception;
    public String readString() throws Exception;
    public Account readAccount() throws Exception;
}