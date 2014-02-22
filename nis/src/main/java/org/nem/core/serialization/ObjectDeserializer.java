package org.nem.core.serialization;

import org.nem.core.crypto.Signature;
import org.nem.core.model.Address;
import org.nem.core.model.Account;

/**
 * An interface for forward-only deserialization of primitive and complex data types.
 * Implementations may use or ignore label parameters but label-based lookup is not guaranteed.
 */
public interface ObjectDeserializer extends Deserializer {

    /**
     * Reads an address object.
     *
     * @param label The optional label.
     * @return The read object.
     */
    public Address readAddress(final String label);

    /**
     * Reads an account object.
     *
     * @param label The optional label.
     * @return The read object.
     */
    public Account readAccount(final String label);

    /**
     * Reads a signature object.
     *
     * @param label The optional label.
     * @return The read object.
     */
    public Signature readSignature(final String label);
}