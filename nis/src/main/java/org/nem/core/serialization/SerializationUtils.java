package org.nem.core.serialization;

import org.nem.core.crypto.Signature;
import org.nem.core.model.Account;
import org.nem.core.model.*;

public class SerializationUtils {

    //region Address

    /**
     * Writes an address object.
     *
     * @param serializer The serializer to use.
     * @param label The optional label.
     * @param address The object.
     */
    public static void writeAddress(final Serializer serializer, String label, final Address address) {
        serializer.writeString(label, address.getEncoded());
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

    //endregion

    //region Account

    /**
     * Writes an account object.
     *
     * @param serializer The serializer to use.
     * @param label The optional label.
     * @param account The object.
     */
    public static void writeAccount(final Serializer serializer, final String label, final Account account) {
        writeAccount(serializer, label, account, AccountEncoding.ADDRESS);
    }

    /**
     * Writes an account object.
     *
     * @param serializer The serializer to use.
     * @param label The optional label.
     * @param account The object.
     * @param encoding The account encoding mode.
     */
    public static void writeAccount(final Serializer serializer, final String label, final Account account, final AccountEncoding encoding) {
        switch (encoding){
            case PUBLIC_KEY:
                serializer.writeBytes(label, account.getKeyPair().getPublicKey());
                break;

            case ADDRESS:
            default:
                writeAddress(serializer, label, account.getAddress());
                break;
        }
    }

    /**
     * Reads an account object.
     *
     * @param deserializer The deserializer to use.
     * @param label The optional label.
     * @return The read object.
     */
    public static Account readAccount(final Deserializer deserializer, final String label) {
        return readAccount(deserializer, label, AccountEncoding.ADDRESS);
    }

    /**
     * Reads an account object.
     *
     * @param deserializer The deserializer to use.
     * @param encoding The account encoding.
     * @param label The optional label.
     * @return The read object.
     */
    public static Account readAccount(final Deserializer deserializer, final String label, final AccountEncoding encoding) {
        Address address;
        switch (encoding){
            case PUBLIC_KEY:
                address = Address.fromPublicKey(deserializer.readBytes(label));
                break;

            case ADDRESS:
            default:
                address = readAddress(deserializer, label);
                break;
        }

        return deserializer.getContext().findAccountByAddress(address);
    }

    //endregion

    //region Signature

    /**
     * Writes a signature object.
     *
     * @param serializer The serializer to use.
     * @param label The optional label.
     * @param signature The object.
     */
    public static void writeSignature(final Serializer serializer, final String label, final Signature signature) {
        serializer.writeBytes(label, signature.getBytes());
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

    //endregion
}
