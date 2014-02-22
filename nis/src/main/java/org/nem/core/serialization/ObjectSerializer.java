package org.nem.core.serialization;

import org.nem.core.crypto.Signature;
import org.nem.core.model.Account;
import org.nem.core.model.Address;

/**
 * An interface for forward-only serialization of primitive and complex data types.
 * Implementations may use or ignore label parameters but label-based lookup is not guaranteed.
 */
public interface ObjectSerializer extends Serializer {

    /**
     * Writes an address object.
     *
     * @param label The optional label.
     * @param address The object.
     */
    public void writeAddress(final String label, final Address address);

    /**
     * Writes an account object.
     *
     * @param label The optional label.
     * @param account The object.
     */
    public void writeAccount(final String label, final Account account);

    /**
     * Writes a signature object.
     *
     * @param label The optional label.
     * @param signature The object.
     */
    public void writeSignature(final String label, final Signature signature);
}