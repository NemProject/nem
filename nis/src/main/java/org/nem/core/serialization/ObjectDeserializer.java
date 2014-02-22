package org.nem.core.serialization;

import org.nem.core.crypto.Signature;
import org.nem.core.model.Address;
import org.nem.core.model.Account;

/**
 * An interface for forward-only deserialization of primitive and complex data types.
 * Implementations may use or ignore label parameters but label-based lookup is not guaranteed.
 */
public interface ObjectDeserializer extends Deserializer {
    public Address readAddress(final String label) throws Exception;
    public Account readAccount(final String label) throws Exception;
    public Signature readSignature(final String label) throws Exception;
}