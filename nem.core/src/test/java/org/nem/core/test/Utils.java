package org.nem.core.test;

import net.minidev.json.JSONObject;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.*;
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
	private static byte[] incrementAtIndex(final byte[] bytes, final int index) {
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

	//region createMosaicDefinition

	/**
	 * Creates a default mosaic definition.
	 *
	 * @param namespaceId The namespace id.
	 * @param name The name.
	 * @return The mosaic definition.
	 */
	public static MosaicDefinition createMosaicDefinition(final String namespaceId, final String name) {
		return createMosaicDefinition(
				generateRandomAccount(),
				Utils.createMosaicId(namespaceId, name),
				createMosaicProperties());
	}

	/**
	 * Creates a default mosaic definition.
	 *
	 * @param creator The creator.
	 * @return The mosaic definition.
	 */
	public static MosaicDefinition createMosaicDefinition(final Account creator) {
		return createMosaicDefinition(
				creator,
				Utils.createMosaicId("alice.vouchers", "alice's gift vouchers"),
				createMosaicProperties());
	}

	/**
	 * Creates a default mosaic definition.
	 *
	 * @param creator The creator.
	 * @param levy The mosaic levy.
	 * @return The mosaic definition.
	 */
	public static MosaicDefinition createMosaicDefinition(final Account creator, final MosaicLevy levy) {
		return createMosaicDefinition(
				creator,
				Utils.createMosaicId("alice.vouchers", "alice's gift vouchers"),
				createMosaicProperties(),
				levy);
	}

	/**
	 * Creates a default mosaic definition.
	 *
	 * @param creator The creator.
	 * @param mosaicId The mosaic id.
	 * @param properties The mosaic properties.
	 * @return The mosaic definition.
	 */
	public static MosaicDefinition createMosaicDefinition(
			final Account creator,
			final MosaicId mosaicId,
			final MosaicProperties properties) {
		return new MosaicDefinition(
				creator,
				mosaicId,
				new MosaicDescriptor("precious vouchers"),
				properties,
				null);
	}

	/**
	 * Creates a default mosaic definition.
	 *
	 * @param creator The creator.
	 * @param mosaicId The mosaic id.
	 * @param properties The mosaic properties.
	 * @param levy The mosaic levy.
	 * @return The mosaic definition.
	 */
	public static MosaicDefinition createMosaicDefinition(
			final Account creator,
			final MosaicId mosaicId,
			final MosaicProperties properties,
			final MosaicLevy levy) {
		return new MosaicDefinition(
				creator,
				mosaicId,
				new MosaicDescriptor("precious vouchers"),
				properties,
				levy);
	}

	/**
	 * Creates a mosaic definition that conforms to a certain pattern.
	 *
	 * @param id The integer id to use.
	 * @return The mosaic definition.
	 */
	public static MosaicDefinition createMosaicDefinition(final int id) {
		return createMosaicDefinition(id, createMosaicProperties());
	}

	/**
	 * Creates a mosaic definition with the specified id and properties.
	 *
	 * @param id The integer id to use.
	 * @param properties The properties.
	 * @return The mosaic definition.
	 */
	public static MosaicDefinition createMosaicDefinition(final int id, final MosaicProperties properties) {
		return createMosaicDefinition(
				generateRandomAccount(),
				createMosaicId(id),
				properties);
	}

	/**
	 * Creates a mosaic definition that conforms to a certain pattern.
	 *
	 * @param namespaceId The namespace id.
	 * @param id The integer id to use.
	 * @return The mosaic definition.
	 */
	public static MosaicDefinition createMosaicDefinition(final NamespaceId namespaceId, final int id) {
		return createMosaicDefinition(
				generateRandomAccount(),
				createMosaicId(namespaceId, id),
				createMosaicProperties());
	}

	/**
	 * Creates a mosaic definition that conforms to a certain pattern.
	 *
	 * @param namespaceId The namespace id.
	 * @param id The integer id to use.
	 * @param properties The properties.
	 * @return The mosaic definition.
	 */
	public static MosaicDefinition createMosaicDefinition(final NamespaceId namespaceId, final int id, final MosaicProperties properties) {
		return createMosaicDefinition(
				generateRandomAccount(),
				createMosaicId(namespaceId, id),
				properties);
	}

	//endregion

	//region createMosaicProperties

	/**
	 * Creates default mosaic properties.
	 *
	 * @return The properties.
	 */
	public static MosaicProperties createMosaicProperties() {
		return createMosaicProperties(0L, 3, null, null);
	}

	/**
	 * Creates mosaic properties with initial supply.
	 *
	 * @param initialSupply The initial supply.
	 * @return The properties.
	 */
	public static MosaicProperties createMosaicPropertiesWithInitialSupply(final Long initialSupply) {
		return createMosaicProperties(initialSupply, 3, null, null);
	}

	/**
	 * Creates custom mosaic properties.
	 *
	 * @param initialSupply The initial supply.
	 * @param divisibility The divisibility.
	 * @param isSupplyMutable A value indicating whether or not the supply is mutable.
	 * @param isTransferable A value indicating whether or not the mosaic is transferable.
	 * @return The properties.
	 */
	public static MosaicProperties createMosaicProperties(
			final Long initialSupply,
			final Integer divisibility,
			final Boolean isSupplyMutable,
			final Boolean isTransferable) {
		final Properties properties = new Properties();
		if (null != initialSupply) {
			properties.put("initialSupply", Long.toString(initialSupply));
		}

		if (null != divisibility) {
			properties.put("divisibility", Long.toString(divisibility));
		}

		if (null != isSupplyMutable) {
			properties.put("supplyMutable", Boolean.toString(isSupplyMutable));
		}

		if (null != isTransferable) {
			properties.put("transferable", Boolean.toString(isTransferable));
		}

		return new DefaultMosaicProperties(properties);
	}

	//endregion

	//region createMosaicId

	/**
	 * Creates a mosaic id that conforms to a certain pattern.
	 *
	 * @param id The integer id to use.
	 * @return The mosaic id.
	 */
	public static MosaicId createMosaicId(final int id) {
		return createMosaicId(new NamespaceId(String.format("id%d", id)), id);
	}

	/**
	 * Creates a mosaic id that conforms to a certain pattern.
	 *
	 * @param namespaceId The namespace id.
	 * @param id The integer id to use.
	 * @return The mosaic id.
	 */
	public static MosaicId createMosaicId(final NamespaceId namespaceId, final int id) {
		return new MosaicId(namespaceId, String.format("name%d", id));
	}

	/**
	 * Creates a default mosaic id.
	 *
	 * @param namespaceId The namespace id.
	 * @param name The name.
	 * @return The mosaic id.
	 */
	public static MosaicId createMosaicId(final String namespaceId, final String name) {
		return new MosaicId(new NamespaceId(namespaceId), name);
	}

	//endregion

	//region createMosaic

	/**
	 * Creates a mosaic that conforms to a certain pattern.
	 *
	 * @param id The integer id to use.
	 * @return The mosaic.
	 */
	public static Mosaic createMosaic(final int id) {
		return new Mosaic(createMosaicId(id), Quantity.fromValue(id));
	}

	/**
	 * Creates a mosaic that conforms to a certain pattern.
	 *
	 * @param id The integer id to use.
	 * @param quantity The quantity.
	 * @return The mosaic.
	 */
	public static Mosaic createMosaic(final int id, final long quantity) {
		return new Mosaic(createMosaicId(id), Quantity.fromValue(quantity));
	}

	/**
	 * Creates a mosaic.
	 *
	 * @param namespaceId The namespace id.
	 * @param name The name.
	 * @return The mosaic.
	 */
	public static Mosaic createMosaic(final String namespaceId, final String name) {
		return new Mosaic(createMosaicId(namespaceId, name), new Quantity(1000));
	}

	//endregion

	//region createMosaicLevy

	/**
	 * Creates a mosaic levy.
	 *
	 * @return The mosaic levy.
	 */
	public static MosaicLevy createMosaicLevy() {
		return createMosaicLevy(Utils.createMosaicId(2));
	}

	/**
	 * Creates a XEM mosaic levy.
	 *
	 * @param mosaicId The mosaic id.
	 * @return The mosaic levy.
	 */
	public static MosaicLevy createMosaicLevy(final MosaicId mosaicId) {
		return new MosaicLevy(
				MosaicTransferFeeType.Absolute,
				generateRandomAccount(),
				mosaicId,
				Quantity.fromValue(123));
	}

	//endregion

	//region nem globals

	/**
	 * Sets up nem globals for testing.
	 */
	public static void setupGlobals() {
		final MosaicFeeInformation feeInfo = new MosaicFeeInformation(Supply.fromValue(100_000_000), 3);
		NemGlobals.setTransactionFeeCalculator(new TransactionFeeCalculatorBeforeFork(id -> feeInfo));
	}

	/**
	 * Resets nem globals.
	 */
	public static void resetGlobals() {
		NemGlobals.setTransactionFeeCalculator(null);
		NemGlobals.setBlockChainConfiguration(null);
	}

	//endregion

	//region block chain configuration

	/**
	 * Creates a new block chain configuration.
	 *
	 * @param maxTransactionsPerSyncAttempt The maximum number of transactions that a remote peer supplies in a chain part.
	 * @param maxTransactionsPerBlock The maximum number of transactions allowed in a single block.
	 * @param blockGenerationTargetTime The target time between two blocks in seconds.
	 * @param blockChainRewriteLimit The block chain rewrite limit.
	 */
	public static BlockChainConfiguration createBlockChainConfiguration(
			final int maxTransactionsPerSyncAttempt,
			final int maxTransactionsPerBlock,
			final int blockGenerationTargetTime,
			final int blockChainRewriteLimit) {
		return new BlockChainConfigurationBuilder()
				.setMaxTransactionsPerSyncAttempt(maxTransactionsPerSyncAttempt)
				.setMaxTransactionsPerBlock(maxTransactionsPerBlock)
				.setBlockGenerationTargetTime(blockGenerationTargetTime)
				.setBlockChainRewriteLimit(blockChainRewriteLimit)
				.build();
	}

	/**
	 * Creates a new block chain configuration.
	 *
	 * @param maxTransactionsPerSyncAttempt The maximum number of transactions that a remote peer supplies in a chain part.
	 * @param maxTransactionsPerBlock The maximum number of transactions allowed in a single block.
	 * @param blockGenerationTargetTime The target time between two blocks in seconds.
	 * @param blockChainRewriteLimit The block chain rewrite limit.
	 * @param blockChainFeatures The block chain features.
	 */
	public static BlockChainConfiguration createBlockChainConfiguration(
			final int maxTransactionsPerSyncAttempt,
			final int maxTransactionsPerBlock,
			final int blockGenerationTargetTime,
			final int blockChainRewriteLimit,
			final BlockChainFeature[] blockChainFeatures) {
		return new BlockChainConfigurationBuilder()
				.setMaxTransactionsPerSyncAttempt(maxTransactionsPerSyncAttempt)
				.setMaxTransactionsPerBlock(maxTransactionsPerBlock)
				.setBlockGenerationTargetTime(blockGenerationTargetTime)
				.setBlockChainRewriteLimit(blockChainRewriteLimit)
				.setBlockChainFeatures(blockChainFeatures)
				.build();
	}

	//endregion
}
