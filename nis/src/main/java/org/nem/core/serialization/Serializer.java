package org.nem.core.serialization;

import org.nem.core.model.Account;

public interface Serializer {
    public void writeInt(final int i) throws Exception;
    public void writeLong(final long l) throws Exception;
    public void writeBytes(final byte[] bytes) throws Exception;
    public void writeString(final String s) throws Exception;
    public void writeAccount(final Account account) throws Exception;
}