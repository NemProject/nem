package org.nem.core.serialization;

import org.nem.core.crypto.Signature;
import org.nem.core.model.Account;

public interface ObjectSerializer extends Serializer {
    public void writeAccount(final Account account) throws Exception;
    public void writeSignature(final Signature signature) throws Exception;
}