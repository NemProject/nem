package org.nem.core.transactions;

import org.nem.core.model.*;
import org.nem.core.serialization.*;

import java.security.InvalidParameterException;

/**
 * Factory class that can deserialize all known transactions.
 */
public class TransactionFactory {
    /**
     * Deserializes a transaction.
     *
     * @param deserializer The deserializer.
     * @return The deserialized transaction.
     */
    public static Transaction Deserialize(ObjectDeserializer deserializer) {
        int type = deserializer.readInt("type");

        switch (type) {
            case TransactionTypes.TRANSFER:
                return new TransferTransaction(deserializer);
        }

        throw new InvalidParameterException("Unknown transaction type: " + type);
    }
}
