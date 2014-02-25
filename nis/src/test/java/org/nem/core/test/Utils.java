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
     * Generates a random address.
     *
     * @return A random address.
     */
    public static Address generateRandomAddress() {
        return new Account(new KeyPair()).getAddress();
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
    public static <T extends VerifiableEntity> ObjectDeserializer RoundtripVerifiableEntity(
        final T originalEntity,
        final Account deserializedSigner) {
        // Arrange:
        final MockAccountLookup accountLookup = new MockAccountLookup();
        accountLookup.setMockAccount(deserializedSigner);

        // Act:
        return RoundtripVerifiableEntity(originalEntity, accountLookup);
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
    public static <T extends VerifiableEntity> ObjectDeserializer RoundtripVerifiableEntity(
        final T originalEntity,
        final AccountLookup accountLookup) {
        // Arrange:
        originalEntity.sign();

        // Act:
        JsonSerializer jsonSerializer = new JsonSerializer(true);
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);
        originalEntity.serialize(serializer);

        return new DelegatingObjectDeserializer(
            new JsonDeserializer(jsonSerializer.getObject()),
            accountLookup);
    }
}
