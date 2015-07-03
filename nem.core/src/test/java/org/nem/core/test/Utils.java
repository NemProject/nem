package org.nem.core.test;

import net.minidev.json.JSONObject;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.GenericAmount;
import org.nem.core.serialization.*;
import org.nem.core.time.*;
import org.nem.core.utils.ExceptionUtils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Properties;

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
		final KeyPair pair = new KeyPair();
		return pair.getPublicKey();
	}

	/**
	 * Generates a random hash.
	 *
	 * @return A random hash.
	 */
	public static Hash generateRandomHash() {
		final byte[] bytes = Utils.generateRandomBytes(32);
		return new Hash(bytes);
	}

	public static Long generateRandomId() {
		final SecureRandom rand = new SecureRandom();
		return rand.nextLong();
	}

	/**
	 * Generates a random signature.
	 *
	 * @return A random signature.
	 */
	public static Signature generateRandomSignature() {
		final byte[] bytes = Utils.generateRandomBytes(64);
		return new Signature(bytes);
	}

	/**
	 * Generates a random time stamp.
	 *
	 * @return A random time stamp.
	 */
	public static TimeInstant generateRandomTimeStamp() {
		final SecureRandom rand = new SecureRandom();
		return new TimeInstant(rand.nextInt(1_000_000));
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
	 * @return A byte array containing random data.
	 */
	public static byte[] generateRandomBytes(final int numBytes) {
		final SecureRandom rand = new SecureRandom();
		final byte[] input = new byte[numBytes];
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
		return new Account(new KeyPair(account.getAddress().getPublicKey()));
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
	 * @param s The string
	 * @param index The index of the character to increment
	 * @return The resulting string
	 */
	public static String incrementAtIndex(final String s, final int index) {
		final char[] chars = s.toCharArray();
		chars[index] = (char)(chars[index] + 1);
		return new String(chars);
	}

	/**
	 * Changes a single character in the specified base 32 string.
	 *
	 * @param s A base 32 string
	 * @param index The index of the character to change
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
	 * @return The resulting byte array
	 */
	public static byte[] incrementAtIndex(final byte[] bytes, final int index) {
		final byte[] copy = new byte[bytes.length];
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
		final StringBuilder builder = new StringBuilder();
		for (int i = 0; i < numChars; ++i) {
			builder.append(ch);
		}

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
	 * Serializes originalEntity and returns a Deserializer that can deserialize it.
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
		final JsonSerializer jsonSerializer = new JsonSerializer(true);
		originalEntity.serialize(jsonSerializer);
		return new JsonDeserializer(jsonSerializer.getObject(), new DeserializationContext(accountLookup));
	}

	/**
	 * Serializes originalEntity and returns a Deserializer that can deserialize it.
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
		final JsonSerializer jsonSerializer = new JsonSerializer(true);
		originalEntity.serialize(jsonSerializer);
		return new JsonDeserializer(jsonSerializer.getObject(), new DeserializationContext(accountLookup));
	}

	/**
	 * Serializes originalEntity and returns a binary Deserializer that can deserialize it.
	 *
	 * @param originalEntity The original entity.
	 * @param accountLookup The account lookup policy to use.
	 * @param <T> The concrete SerializableEntity type.
	 * @return The binary object deserializer.
	 */
	public static <T extends SerializableEntity> Deserializer roundtripSerializableEntityWithBinarySerializer(
			final T originalEntity,
			final AccountLookup accountLookup) {
		// Act:
		final BinarySerializer binarySerializer = new BinarySerializer();
		originalEntity.serialize(binarySerializer);
		return new BinaryDeserializer(binarySerializer.getBytes(), new DeserializationContext(accountLookup));
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
	 * @return A slightly different key
	 */
	public static PublicKey mutate(final PublicKey key) {
		return new PublicKey(Utils.incrementAtIndex(key.getRaw(), 12));
	}

	/**
	 * Mutates key into a slightly different key.
	 *
	 * @param key The original key.
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
	 * Creates a time provider that returns the specified instants.
	 *
	 * @param rawInstants The raw instant values.
	 * @return The time provider.
	 */
	public static TimeProvider createMockTimeProvider(final int... rawInstants) {
		final TimeInstant[] instants = new TimeInstant[rawInstants.length - 1];
		for (int i = 1; i < rawInstants.length; ++i) {
			instants[i - 1] = new TimeInstant(rawInstants[i]);
		}

		final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		Mockito.when(timeProvider.getCurrentTime()).thenReturn(new TimeInstant(rawInstants[0]), instants);
		return timeProvider;
	}

	/**
	 * Creates a default mosaic.
	 *
	 * @param creator The mosaic creator.
	 * @return The mosaic.
	 */
	public static Mosaic createMosaic(final Account creator) {
		return createMosaic(creator, createMosaicProperties());
	}

	/**
	 * Creates a default mosaic.
	 *
	 * @param creator The mosaic creator.
	 * @param properties The mosaic properties.
	 * @return The mosaic.
	 */
	public static Mosaic createMosaic(final Account creator, final MosaicProperties properties) {
		return new Mosaic(
				creator,
				new MosaicId(new NamespaceId("alice.vouchers"), "Alice's gift vouchers"),
				new MosaicDescriptor("precious vouchers"),
				GenericAmount.fromValue(123),
				properties);
	}

	public static MosaicProperties createMosaicProperties() {
		final Properties properties = new Properties();
		properties.put("divisibility", "3");
		return new MosaicPropertiesImpl(properties);
	}
}
