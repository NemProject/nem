package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.observers.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

public class SmartTileSupplyChangeTransactionTest {
	private static final long MAX_QUANTITY = MosaicConstants.MAX_QUANTITY;
	private static final Account SIGNER = Utils.generateRandomAccount();
	private static final TimeInstant TIME_INSTANT = new TimeInstant(123);
	private static final MosaicId MOSAIC_ID = new MosaicId(new NamespaceId("foo.bar"), "baz");

	// region ctor

	@Test
	public void canCreateSmartTileSupplyChangeTransactionForCreatingSmartTiles() {
		// Assert:
		assertCanCreateTransaction(MOSAIC_ID, SmartTileSupplyType.CreateSmartTiles, Quantity.fromValue(100));
	}

	@Test
	public void canCreateSmartTileSupplyChangeTransactionForDeletingSmartTiles() {
		// Assert:
		assertCanCreateTransaction(MOSAIC_ID, SmartTileSupplyType.DeleteSmartTiles, Quantity.fromValue(100));
	}

	@Test
	public void canCreateSmartTileSupplyChangeTransactionWithMaximumAllowedQuantity() {
		// Assert:
		assertCanCreateTransaction(MOSAIC_ID, SmartTileSupplyType.DeleteSmartTiles, Quantity.fromValue(MAX_QUANTITY));
	}

	@Test
	public void cannotCreateTransactionWithNullParameters() {
		// Assert
		Arrays.asList("mosaicId", "supplyType", "quantity")
				.forEach(SmartTileSupplyChangeTransactionTest::assertMosaicCannotBeCreatedWithNull);
	}

	@Test
	public void cannotCreateTransactionWithUnknownSupplyType() {
		// Assert:
		assertCannotCreateTransaction(MOSAIC_ID, SmartTileSupplyType.Unknown, Quantity.fromValue(100));
	}

	@Test
	public void cannotCreateTransactionWithZeroQuantity() {
		// Assert:
		assertCannotCreateTransaction(MOSAIC_ID, SmartTileSupplyType.CreateSmartTiles, Quantity.ZERO);
	}

	@Test
	public void cannotCreateTransactionWithOutOfRangeQuantity() {
		// Assert:
		assertCannotCreateTransaction(
				MOSAIC_ID,
				SmartTileSupplyType.CreateSmartTiles,
				Quantity.fromValue(MAX_QUANTITY + 1));
	}

	private static void assertCanCreateTransaction(
			final MosaicId mosaicId,
			final SmartTileSupplyType supplyType,
			final Quantity quantity) {
		// Act:
		final SmartTileSupplyChangeTransaction transaction = createTransaction(mosaicId, supplyType, quantity);

		// Assert:
		assertSuperClassValues(transaction);
		Assert.assertThat(transaction.getMosaicId(), IsEqual.equalTo(mosaicId));
		Assert.assertThat(transaction.getSupplyType(), IsEqual.equalTo(supplyType));
		Assert.assertThat(transaction.getQuantity(), IsEqual.equalTo(quantity));
	}

	private static void assertCannotCreateTransaction(
			final MosaicId mosaicId,
			final SmartTileSupplyType supplyType,
			final Quantity quantity) {
		ExceptionAssert.assertThrows(v -> createTransaction(mosaicId, supplyType, quantity), IllegalArgumentException.class);
	}

	private static void assertMosaicCannotBeCreatedWithNull(final String parameterName) {
		ExceptionAssert.assertThrows(
				v -> new SmartTileSupplyChangeTransaction(
						TIME_INSTANT,
						SIGNER,
						parameterName.equals("mosaicId") ? null : MOSAIC_ID,
						parameterName.equals("supplyType") ? null : SmartTileSupplyType.CreateSmartTiles,
						parameterName.equals("quantity") ? null : Quantity.fromValue(123L)),
				IllegalArgumentException.class,
				ex -> ex.getMessage().contains(parameterName));
	}

	// endregion

	// region getOtherAccounts

	@Test
	public void getOtherAccountsReturnsEmptyList() {
		// Arrange:
		final SmartTileSupplyChangeTransaction transaction = createTransaction();

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
		final SmartTileSupplyChangeTransaction transaction = createTransaction();

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
		final SmartTileSupplyChangeTransaction original = createTransaction();

		// Act:
		final SmartTileSupplyChangeTransaction transaction = createRoundTrippedTransaction(original);

		assertSuperClassValues(transaction);
		Assert.assertThat(transaction.getMosaicId(), IsEqual.equalTo(MOSAIC_ID));
		Assert.assertThat(transaction.getSupplyType(), IsEqual.equalTo(SmartTileSupplyType.CreateSmartTiles));
		Assert.assertThat(transaction.getQuantity(), IsEqual.equalTo(Quantity.fromValue(123)));
	}

	@Test
	public void cannotDeserializeTransactionWithMissingRequiredParameter() {
		// Assert:
		Arrays.asList("mosaicId", "supplyType", "quantity")
				.forEach(p -> SmartTileSupplyChangeTransactionTest.assertCannotDeserialize(p, null));
	}

	@Test
	public void cannotDeserializeTransactionWithOutOfRangeParameter() {
		// Assert:
		assertCannotDeserialize("supplyType", 0);
		assertCannotDeserialize("quantity", Quantity.ZERO);
		assertCannotDeserialize("quantity", Quantity.fromValue(MAX_QUANTITY + 1));
	}

	private static void assertCannotDeserialize(final String propertyName, final Object propertyValue) {
		// Arrange:
		final SmartTileSupplyChangeTransaction transaction = createTransaction();
		final JSONObject jsonObject = JsonSerializer.serializeToJson(transaction.asNonVerifiable());
		if (null == propertyValue) {
			jsonObject.remove(propertyName);
		} else {
			jsonObject.put(propertyName, propertyValue);
		}

		final JsonDeserializer deserializer = new JsonDeserializer(jsonObject, new DeserializationContext(new MockAccountLookup()));
		deserializer.readInt("type");

		// Assert:
		ExceptionAssert.assertThrows(
				v -> new MosaicCreationTransaction(VerifiableEntity.DeserializationOptions.NON_VERIFIABLE, deserializer),
				MissingRequiredPropertyException.class);
	}

	private static SmartTileSupplyChangeTransaction createRoundTrippedTransaction(final Transaction originalTransaction) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalTransaction.asNonVerifiable(), new MockAccountLookup());
		deserializer.readInt("type");
		return new SmartTileSupplyChangeTransaction(VerifiableEntity.DeserializationOptions.NON_VERIFIABLE, deserializer);
	}

	// endregion

	//region execute / undo

	@Test
	public void executeRaisesAppropriateNotifications() {
		// Arrange:
		final SmartTileSupplyChangeTransaction transaction = createTransaction();
		transaction.setFee(Amount.fromNem(100));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.execute(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(2)).notify(notificationCaptor.capture());
		final List<Notification> values = notificationCaptor.getAllValues();
		NotificationUtils.assertSmartTileSupplyChangeNotification(
				values.get(0),
				transaction.getSigner(),
				new SmartTile(MOSAIC_ID, Quantity.fromValue(123)),
				SmartTileSupplyType.CreateSmartTiles);
		NotificationUtils.assertBalanceDebitNotification(values.get(1), SIGNER, Amount.fromNem(100));
	}

	@Test
	public void undoRaisesAppropriateNotifications() {
		// Arrange:
		final SmartTileSupplyChangeTransaction transaction = createTransaction();
		transaction.setFee(Amount.fromNem(100));

		// Act:
		final TransactionObserver observer = Mockito.mock(TransactionObserver.class);
		transaction.undo(observer);

		// Assert:
		final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		Mockito.verify(observer, Mockito.times(2)).notify(notificationCaptor.capture());
		final List<Notification> values = notificationCaptor.getAllValues();
		NotificationUtils.assertBalanceCreditNotification(values.get(0), SIGNER, Amount.fromNem(100));
		NotificationUtils.assertSmartTileSupplyChangeNotification(
				values.get(1),
				transaction.getSigner(),
				new SmartTile(MOSAIC_ID, Quantity.fromValue(123)),
				SmartTileSupplyType.CreateSmartTiles);
	}

	// endregion

	private static void assertSuperClassValues(final Transaction transaction) {
		// Assert:
		Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.SMART_TILE_SUPPLY_CHANGE));
		Assert.assertThat(transaction.getVersion(), IsEqual.equalTo(VerifiableEntityUtils.VERSION_ONE));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(TIME_INSTANT));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(SIGNER));
		Assert.assertThat(transaction.getDebtor(), IsEqual.equalTo(SIGNER));
	}

	private static SmartTileSupplyChangeTransaction createTransaction() {
		return createTransaction(MOSAIC_ID, SmartTileSupplyType.CreateSmartTiles, Quantity.fromValue(123));
	}

	private static SmartTileSupplyChangeTransaction createTransaction(
			final MosaicId mosaicId,
			final SmartTileSupplyType supplyType,
			final Quantity quantity) {
		return new SmartTileSupplyChangeTransaction(
				TIME_INSTANT,
				SIGNER,
				mosaicId,
				supplyType,
				quantity);
	}
}
