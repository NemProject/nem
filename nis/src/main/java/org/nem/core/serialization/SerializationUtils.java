package org.nem.core.serialization;

import org.nem.core.crypto.Signature;
import org.nem.core.model.Account;
import org.nem.core.model.*;

public class SerializationUtils {

    /**
     * Writes an address object.
     *
     * @param label The serializer to use.
     * @param address The object.
     */
    public static void writeAddress(final Serializer serializer, String label, final Address address) {
        serializer.writeString(label, address.getEncoded());
    }

    /**
     * Writes an account object.
     *
     * @param label The serializer to use.
     * @param account The object.
     */
    public static void writeAccount(final Serializer serializer, final String label, final Account account) {
        writeAddress(serializer, label, account.getAddress());
    }

    /**
     * Writes a signature object.
     *
     * @param label The serializer to use.
     * @param signature The object.
     */
    public static void writeSignature(final Serializer serializer, final String label, final Signature signature) {
        serializer.writeBytes(label, signature.getBytes());
    }

    /**
     * Reads an address object.
     *
     * @param deserializer The deserializer to use.
     * @param label The optional label.
     * @return The read object.
     */
    public static Address readAddress(final Deserializer deserializer, final String label) {
        String encodedAddress = deserializer.readString(label);
        return Address.fromEncoded(encodedAddress);
    }

    /**
     * Reads an account object.
     *
     * @param deserializer The deserializer to use.
     * @param label The optional label.
     * @return The read object.
     */
    public static Account readAccount(final Deserializer deserializer, final String label) {
        Address address = readAddress(deserializer, label);
        return deserializer.getContext().findAccountByAddress(address);
    }

    /**
     * Reads a signature object.
     *
     * @param deserializer The deserializer to use.
     * @param label The optional label.
     * @return The read object.
     */
    public static Signature readSignature(final Deserializer deserializer, final String label) {
        byte[] bytes = deserializer.readBytes(label);
        return new Signature(bytes);
    }
}
