package org.nem.core.serialization;

import org.nem.core.crypto.Signature;
import org.nem.core.model.Account;

/**
 * An interface for forward-only serialization of primitive and complex data types.
 * Implementations may use or ignore label parameters but label-based lookup is not guaranteed.
 */
public interface ObjectSerializer extends Serializer {
    public void writeAccount(final String label, final Account account) throws Exception;
    public void writeSignature(final String label, final Signature signature) throws Exception;
}