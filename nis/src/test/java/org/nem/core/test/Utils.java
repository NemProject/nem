package org.nem.core.test;

import org.nem.core.crypto.KeyPair;
import org.nem.core.model.*;
import org.nem.core.serialization.*;

import java.security.SecureRandom;

/**
 * Static class containing test utilities.
 */
public class Utils {

    /**
     * Generates a byte array containing random data.
     */
    public static byte[] generateRandomBytes() {
        return generateRandomBytes(214);
    }

    /**
     * Generates a byte array containing random data.
     *
     * @param numBytes The number of bytes to generate.
     */
    public static byte[] generateRandomBytes(int numBytes) {
        SecureRandom rand = new SecureRandom();
        byte[] input = new byte[numBytes];
        rand.nextBytes(input);
        return input;
    }

    /**
     * Creates a copy of account that only contains the account public key.
     *
     * @param account The account to copy.
     * @return A copy of account that only contains the account public key.
     */
    public static Account createPublicOnlyKeyAccount(final Account account) {
        return new Account(new KeyPair(account.getKeyPair().getPublicKey()));
    }

    /**
     * Generates a random account.
     *
     * @return A random account.
     */
    public static Account generateRandomAccount() {
        return new Account(new KeyPair());
    }

    /**
     * Generates a random account without a private key.
     *
     * @return A random account without a private key.
     */
    public static Account generateRandomAccountWithoutPrivateKey() {
        return createPublicOnlyKeyAccount(generateRandomAccount());
    }

    /**
     * Generates a random address.
     *
     * @return A random address.
     */
    public static Address generateRandomAddress() {
        return generateRandomAccount().getAddress();
    }

    /**
     * Increments a single character in the specified string.
     *
     * @param s The string
     * @param index The index of the character to increment
     * @return The resulting string
     */
    public static String incrementAtIndex(final String s, final int index) {
        char[] chars = s.toCharArray();
        chars[index] = (char)(chars[index] + 1);
        return new String(chars);
    }

    /**
     * Increments a single byte in the specified byte array.
     *
     * @param bytes The byte array
     * @param index The index of the byte to increment
     * @return The resulting byte array
     */
    public static byte[] incrementAtIndex(final byte[] bytes, final int index) {
        byte[] copy = new byte[bytes.length];
        System.arraycopy(bytes, 0, copy, 0, bytes.length);
        ++copy[index];
        return copy;
    }

    /**
     * Creates a string initialized with a single character.
     *
     * @param ch The character used in the string.
     * @param numChars The number of characters in hte string.
     * @return A string of length numChars initialized to ch.
     */
    public static String createString(final char ch, final int numChars) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < numChars; ++i)
            builder.append(ch);

        return builder.toString();
    }

    /**
     * Serializes originalEntity and returns an ObjectDeserializer
     * that can deserialize it.
     *
     * @param originalEntity The original entity.
     * @param deserializedSigner The signer that should be associated with the deserialized object.
     * @param <T> The concrete VerifiableEntity type.
     * @return The object deserializer.
     */
    public static <T extends VerifiableEntity> Deserializer roundtripVerifiableEntity(
        final T originalEntity,
        final Account deserializedSigner) {
        // Arrange:
        final MockAccountLookup accountLookup = new MockAccountLookup();
        accountLookup.setMockAccount(deserializedSigner);

        // Act:
        return roundtripVerifiableEntity(originalEntity, accountLookup);
    }

    /**
     * Serializes originalEntity and returns an ObjectDeserializer
     * that can deserialize it.
     *
     * @param originalEntity The original entity.
     * @param accountLookup The account lookup policy to use.
     * @param <T> The concrete VerifiableEntity type.
     * @return The object deserializer.
     */
    public static <T extends VerifiableEntity> Deserializer roundtripVerifiableEntity(
        final T originalEntity,
        final AccountLookup accountLookup) {
        // Arrange:
        originalEntity.sign();

        // Act:
        JsonSerializer jsonSerializer = new JsonSerializer(true);
        originalEntity.serialize(jsonSerializer);
        return new JsonDeserializer(jsonSerializer.getObject(), new DeserializationContext(accountLookup));
    }

    /**
     * Serializes serializable and returns an ObjectDeserializer
     * that can deserialize it.
     *
     * @param originalEntity The original entity.
     * @param accountLookup The account lookup policy to use.
     * @param <T> The concrete SerializableEntity type.
     * @return The object deserializer.
     */
    public static <T extends SerializableEntity> Deserializer roundtripSerializableEntity(
        final T originalEntity,
        final AccountLookup accountLookup) {
        // Act:
        JsonSerializer jsonSerializer = new JsonSerializer(true);
        originalEntity.serialize(jsonSerializer);
        return new JsonDeserializer(jsonSerializer.getObject(), new DeserializationContext(accountLookup));
    }
}
