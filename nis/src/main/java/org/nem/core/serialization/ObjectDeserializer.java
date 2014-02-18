package org.nem.core.serialization;

import org.nem.core.crypto.Signature;
import org.nem.core.model.Account;

public interface ObjectDeserializer extends Deserializer {
    public Account readAccount() throws Exception;
    public Signature readSignature() throws Exception;
}