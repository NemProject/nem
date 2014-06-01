package org.nem.core.test;

import net.minidev.json.JSONObject;
import org.nem.core.crypto.Hash;
import org.nem.core.crypto.KeyPair;
import org.nem.core.crypto.PrivateKey;
import org.nem.core.crypto.PublicKey;
import org.nem.core.model.*;
import org.nem.core.serialization.*;
import org.nem.core.utils.ExceptionUtils;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Static class containing test utilities.
 */
public class Utils {

	/**
	 * Generates a random public key.
	 *
	 * @return A random public key.
	 */
	public static PublicKey generateRandomPublicKey() {
		final byte[] bytes = Utils.generateRandomBytes(33);
		bytes[0] = 0x02;
		return new PublicKey(bytes);
	}

	/**
	 * Generates a random hash.
	 *
	 * @return A random hash.
	 */
	public static Hash generateRandomHash() {
		final byte[] bytes = Utils.generateRandomBytes(64);
		return new Hash(bytes);
	}

	/**
	 * Generates a byte array containing random data.
	 *
	 * @return A byte array containing random data.
	 */
	public static byte[] generateRandomBytes() {
		return generateRandomBytes(214);
	}

	/**
	 * Generates a byte array containing random data.
	 *
	 * @param numBytes The number of bytes to generate.
	 *
	 * @return A byte array containing random data.
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
	 *
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
		return Address.fromEncoded(Utils.generateRandomAccount().getAddress().getEncoded());
	}

	/**
	 * Generates a random address with a public key.
	 *
	 * @return A random address.
	 */
	public static Address generateRandomAddressWithPublicKey() {
		return Address.fromPublicKey(Utils.generateRandomPublicKey());
	}

	/**
	 * Increments a single character in the specified string.
	 *
	 * @param s     The string
	 * @param index The index of the character to increment
	 *
	 * @return The resulting string
	 */
	public static String incrementAtIndex(final String s, final int index) {
		char[] chars = s.toCharArray();
		chars[index] = (char)(chars[index] + 1);
		return new String(chars);
	}

	/**
	 * Changes a single character in the specified base 32 string.
	 *
	 * @param s     A base 32 string
	 * @param index The index of the character to change
	 *
	 * @return The resulting base 32 string
	 */
	public static String modifyBase32AtIndex(final String s, final int index) {
		final char[] chars = s.toCharArray();
		final char currentChar = chars[index];

		char newChar = (char)(currentChar + 1);
		switch (currentChar) {
			case 'Z':
			case '7':
				newChar = 'A';
		}

		chars[index] = newChar;
		return new String(chars);
	}

	/**
	 * Increments a single byte in the specified byte array.
	 *
	 * @param bytes The byte array
	 * @param index The index of the byte to increment
	 *
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
	 * @param ch       The character used in the string.
	 * @param numChars The number of characters in hte string.
	 *
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
	 * @param originalEntity     The original entity.
	 * @param deserializedSigner The signer that should be associated with the deserialized object.
	 * @param <T>                The concrete VerifiableEntity type.
	 *
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
	 * @param accountLookup  The account lookup policy to use.
	 * @param <T>            The concrete VerifiableEntity type.
	 *
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
	 * @param accountLookup  The account lookup policy to use.
	 * @param <T>            The concrete SerializableEntity type.
	 *
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

	/**
	 * Waits on the specified monitor.
	 *
	 * @param monitor The monitor.
	 */
	public static void monitorWait(final Object monitor) {
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (monitor) {
			ExceptionUtils.propagateVoid(monitor::wait);
		}
	}

	/**
	 * Signals the specified monitor.
	 *
	 * @param monitor The monitor.
	 */
	public static void monitorSignal(final Object monitor) {
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (monitor) {
			monitor.notifyAll();
		}
	}

	/**
	 * Mutates key into a slightly different key.
	 *
	 * @param key The original key.
	 *
	 * @return A slightly different key
	 */
	public static PublicKey mutate(final PublicKey key) {
		return new PublicKey(Utils.incrementAtIndex(key.getRaw(), 12));
	}

	/**
	 * Mutates key into a slightly different key.
	 *
	 * @param key The original key.
	 *
	 * @return A slightly different key
	 */
	public static PrivateKey mutate(final PrivateKey key) {
		return new PrivateKey(key.getRaw().add(BigInteger.ONE));
	}

	/**
	 * Creates a JsonDeserializer around a JSONObject.
	 *
	 * @param object The json object.
	 * @return The deserializer.
	 */
	public static JsonDeserializer createDeserializer(final JSONObject object) {
		return new JsonDeserializer(
				object,
				new DeserializationContext(new MockAccountLookup()));
	}

	/**
	 * Creates a new account link.
	 *
	 * @param blockHeight The block height.
	 * @param amount The amount.
	 * @param address The address.
	 * @return The account link.
	 */
	public static AccountLink createLink(final int blockHeight, final long amount, final String address) {
		return new AccountLink(
				new BlockHeight(blockHeight),
				Amount.fromNem(amount),
				Address.fromEncoded(address));
	}
}
