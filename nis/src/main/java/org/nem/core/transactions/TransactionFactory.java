package org.nem.core.transactions;

import org.nem.core.model.*;
import org.nem.core.serialization.*;

import java.security.InvalidParameterException;

public class TransactionFactory {
    public static Transaction Deserialize(ObjectDeserializer deserializer) throws Exception {
        int type = deserializer.readInt("type");

        switch (type) {
            case TransactionTypes.TRANSFER:
                return new TransferTransaction(deserializer);
        }

        throw new InvalidParameterException("Unknown transaction type: " + type);
    }
}
