package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.GenericAmount;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

public class MosaicCreationTransactionTest {
	private static final Account SIGNER = Utils.generateRandomAccount();
	private static final TimeInstant TIME_INSTANT = new TimeInstant(123);

	// region ctor

	@Test
	public void canCreateMosaicCreationTransactionFromValidParameters() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final MosaicCreationTransaction transaction = createTransaction(context.mosaic);

		// Assert
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.MOSAIC_CREATION));
		Assert.assertThat(transaction.getVersion(), IsEqual.equalTo(VerifiableEntityUtils.VERSION_ONE));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(TIME_INSTANT));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(SIGNER));
		Assert.assertThat(transaction.getMosaic(), IsSame.sameInstance(context.mosaic));
	}

	@Test
	public void cannotCreateTransactionWithNullMosaic() {
		// Assert
		ExceptionAssert.assertThrows(v -> createTransaction(null), IllegalArgumentException.class);
	}

	@Test
	public void cannotCreateTransactionWithDifferentTransactionSignerAndMosaicCreator() {
		// Arrange:
		final TestContext context = new TestContext(Utils.generateRandomAccount());

		// Assert
		ExceptionAssert.assertThrows(v -> createTransaction(context.mosaic), IllegalArgumentException.class);
	}

	// endregion

	// region getOtherAccounts

	@Test
	public void getOtherAccountsReturnsEmptyList() {
		// Arrange:
		final MosaicCreationTransaction transaction = createTransaction();

		// Act:
		final Collection<Account> accounts = transaction.getOtherAccounts();

		// Assert:
		Assert.assertThat(accounts, IsEqual.equalTo(Collections.emptyList()));
	}

	// endregion

	// region getAccounts

	@Test
	public void getAccountsIncludesSigner() {
		// Arrange:
		final MosaicCreationTransaction transaction = createTransaction();

		// Act:
		final Collection<Account> accounts = transaction.getAccounts();

		// Assert:
		Assert.assertThat(accounts, IsEquivalent.equivalentTo(Collections.singletonList(SIGNER)));
	}

	// endregion

	// region round trip

	@Test
	public void canRoundTripTransaction() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicCreationTransaction original = createTransaction(context.mosaic);

		// Act:
		final MosaicCreationTransaction transaction = createRoundTrippedTransaction(original);
		final Mosaic mosaic = transaction.getMosaic();

		// Assert:
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.MOSAIC_CREATION));
		Assert.assertThat(transaction.getVersion(), IsEqual.equalTo(VerifiableEntityUtils.VERSION_ONE));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(TIME_INSTANT));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(SIGNER));
		Assert.assertThat(mosaic.getCreator(), IsEqual.equalTo(context.mosaic.getCreator()));
		Assert.assertThat(mosaic.getProperties(), IsEquivalent.equivalentTo(context.mosaic.getProperties()));
		Assert.assertThat(mosaic.getAmount(), IsEqual.equalTo(GenericAmount.fromValue(123)));
	}

	@Test
	public void cannotDeserializeTransactionWithMissingRequiredParameter() {
		// Assert:
		assertCannotDeserializeWithMissingProperty("mosaic");
	}

	private static void assertCannotDeserializeWithMissingProperty(final String propertyName) {
		// Arrange:
		final MosaicCreationTransaction transaction = createTransaction();
		final JSONObject jsonObject = JsonSerializer.serializeToJson(transaction.asNonVerifiable());
		jsonObject.remove(propertyName);
		final JsonDeserializer deserializer = new JsonDeserializer(jsonObject, new DeserializationContext(new MockAccountLookup()));
		deserializer.readInt("type");

		// Assert:
		ExceptionAssert.assertThrows(
				v -> new MosaicCreationTransaction(VerifiableEntity.DeserializationOptions.NON_VERIFIABLE, deserializer),
				MissingRequiredPropertyException.class);
	}

	private static MosaicCreationTransaction createRoundTrippedTransaction(final Transaction originalTransaction) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalTransaction.asNonVerifiable(), new MockAccountLookup());
		deserializer.readInt("type");
		return new MosaicCreationTransaction(VerifiableEntity.DeserializationOptions.NON_VERIFIABLE, deserializer);
	}

	// endregion

	private static MosaicProperties createProperties() {
		final Properties properties = new Properties();
		properties.put("name", "Alice's gift vouchers");
		properties.put("namespace", "alice.vouchers");
		return new MosaicPropertiesImpl(properties);
	}

	private static Mosaic createMosaic(
			final Account creator,
			final MosaicProperties properties,
			final GenericAmount amount) {
		return new Mosaic(creator, properties, amount);
	}

	private static MosaicCreationTransaction createTransaction() {
		return new MosaicCreationTransaction(TIME_INSTANT, SIGNER, createMosaic(
				SIGNER,
				createProperties(),
				GenericAmount.fromValue(123)));
	}

	private static MosaicCreationTransaction createTransaction(final Mosaic mosaic) {
		return new MosaicCreationTransaction(TIME_INSTANT, SIGNER, mosaic);
	}

	private class TestContext {
		private final Account creator;
		private final MosaicProperties properties = createProperties();
		private final Mosaic mosaic;

		private TestContext() {
			this(SIGNER);
		}

		private TestContext(final Account creator) {
			this.creator = creator;
			this.mosaic = createMosaic(this.creator, this.properties, GenericAmount.fromValue(123));
		}
	}
}
