package org.nem.nis.validators.transaction;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.test.DebitPredicates;
import org.nem.nis.validators.ValidationContext;

import java.util.*;

public class SmartTileBagValidatorTest {
	private static final Account SIGNER = Utils.generateRandomAccount();
	private static final Account RECIPIENT = Utils.generateRandomAccount();

	@Test
	public void transactionWithEmptyBagValidates() {
		// Arrange:
		final TestContext context = new TestContext();
		final TransferTransaction transaction = createTransaction(Amount.fromNem(1), Collections.emptyList());
		context.addMosaicToCache(context.mosaic);
		context.addSmartTile();

		// Assert:
		assertValidationResultForTransaction(context, transaction, ValidationResult.SUCCESS);
	}

	@Test
	public void validTransactionValidates() {
		// Arrange:
		final TestContext context = new TestContext();
		final TransferTransaction transaction = createTransaction(Amount.fromNem(1), Collections.singletonList(createSmartTile("foo", 10)));
		context.addMosaicToCache(context.mosaic);
		context.addSmartTile();

		// Assert:
		assertValidationResultForTransaction(context, transaction, ValidationResult.SUCCESS);
	}

	@Test
	public void transactionIsInvalidIfAtLSmartTileHasUnknownMosaicId() {
		// Arrange:
		final TestContext context = new TestContext();
		final TransferTransaction transaction = createTransaction(Amount.fromNem(1), Collections.singletonList(createSmartTile("fooo", 10)));

		// Assert:
		assertValidationResultForTransaction(context, transaction, ValidationResult.FAILURE_MOSAIC_UNKNOWN);
	}

	@Test
	public void transactionIsInvalidIfMosaicIsNotTransferableAndSignerIsNotMosaicCreator() {
		// Arrange:
		final TestContext context = new TestContext(Utils.generateRandomAccount(), false);
		final TransferTransaction transaction = createTransaction(Amount.fromNem(1), Collections.singletonList(createSmartTile("foo", 10)));
		context.addMosaicToCache(context.mosaic);

		// Assert:
		assertValidationResultForTransaction(context, transaction, ValidationResult.FAILURE_MOSAIC_NOT_TRANSFERABLE);
	}

	@Test
	public void transactionIsInvalidIfProductOfTransactionAmountAndSmartTileQuantityExceedsLongMax() {
		// Arrange:
		final TestContext context = new TestContext();
		final TransferTransaction transaction = createTransaction(Amount.fromMicroNem(Long.MAX_VALUE), Collections.singletonList(createSmartTile("foo", 10)));
		context.addMosaicToCache(context.mosaic);

		// Assert:
		assertValidationResultForTransaction(context, transaction, ValidationResult.FAILURE_MOSAIC_MAX_QUANTITY_EXCEEDED);
	}

	@Test
	public void transactionIsInvalidIfDivisibilityOfMosaicIsViolated() {
		// Arrange:
		final TestContext context = new TestContext();
		final TransferTransaction transaction = createTransaction(Amount.fromMicroNem(10_000L), Collections.singletonList(createSmartTile("foo", 10)));
		context.addMosaicToCache(context.mosaic);

		// Assert (0.01 * 10 is smaller than 1 smallest smart tile unit):
		assertValidationResultForTransaction(context, transaction, ValidationResult.FAILURE_MOSAIC_DIVISIBILITY_VIOLATED);
	}

	@Test
	public void transactionIsInvalidIfForXemMosaicIfSignerHasNotEnoughXemFunds() {
		// Arrange:
		final TestContext context = new TestContext();
		final TransferTransaction transaction = createTransaction(Amount.fromNem(2), Collections.singletonList(createSmartTile("nem", "xem", 1_000_000L)));
		context.addMosaicToCache(context.mosaic);

		assertValidationResultForTransaction(context, transaction, ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
	}

	@Test
	public void transactionIsInvalidIfForNonXemMosaicIfSignerHasNotEnoughSmartTileQuantity() {
		// Arrange:
		final TestContext context = new TestContext();
		final TransferTransaction transaction = createTransaction(Amount.fromNem(1), Collections.singletonList(createSmartTile("foo", 1_000L)));
		context.addMosaicToCache(context.mosaic);
		context.addSmartTile();

		assertValidationResultForTransaction(context, transaction, ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
	}

	private static void assertValidationResultForTransaction(
			final TestContext context,
			final TransferTransaction transaction,
			final ValidationResult expectedResult) {

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	private static TransferTransaction createTransaction(
			final Amount amount,
			final Collection<SmartTile> smartTiles) {
		return new TransferTransaction(
				2,
				TimeInstant.ZERO,
				SIGNER,
				RECIPIENT,
				amount,
				null,
				new SmartTileBag(smartTiles));
	}

	private static Mosaic createMosaic(
			final Account creator,
			final long quantity,
			final int divisibility,
			final boolean mutableQuantity,
			final boolean transferable) {
		final Properties properties = new Properties();
		properties.put("quantity", String.valueOf(quantity));
		properties.put("divisibility", String.valueOf(divisibility));
		properties.put("mutablequantity", mutableQuantity ? "true" : "false");
		properties.put("transferable", transferable ? "true" : "false");
		final MosaicProperties mosaicProperties = new DefaultMosaicProperties(properties);
		return new Mosaic(
				creator,
				new MosaicId(new NamespaceId("foo"), "bar"),
				new MosaicDescriptor("baz"),
				mosaicProperties);

	}

	private static SmartTile createSmartTile(final String namespace, final long quantity) {
		return createSmartTile(namespace, "bar", quantity);
	}

	private static SmartTile createSmartTile(final String namespace, final String mosaicName, final long quantity) {
		return new SmartTile(new MosaicId(new NamespaceId(namespace), mosaicName), Quantity.fromValue(quantity));
	}

	private class TestContext {
		final Mosaic mosaic;
		final AccountStateCache stateCache = Mockito.mock(AccountStateCache.class);
		private final NamespaceId namespaceId = new NamespaceId("foo");
		private final MosaicId mosaicId = new MosaicId(namespaceId, "bar");
		final NamespaceCache namespaceCache = Mockito.mock(NamespaceCache.class);
		private final NamespaceEntry namespaceEntry = Mockito.mock(NamespaceEntry.class);
		private final Mosaics mosaics = Mockito.mock(Mosaics.class);
		private final MosaicEntry mosaicEntry = Mockito.mock(MosaicEntry.class);
		final AccountState state = Mockito.mock(AccountState.class);
		final AccountInfo accountInfo = Mockito.mock(AccountInfo.class);
		final SmartTileMap map = new SmartTileMap();
		final SmartTileBagValidator validator = new SmartTileBagValidator(this.stateCache, this.namespaceCache);

		private TestContext() {
			this(SIGNER, 1000, 6, true, true);
		}

		private TestContext(final Account creator, final boolean transferable) {
			this(creator, 1000, 6, true, transferable);
		}

		private TestContext(final int divisibility) {
			this(SIGNER, 1000, 1, true, true);
		}

		private TestContext(
				final Account creator,
				final long quantity,
				final int divisibility,
				final boolean mutableQuantity,
				final boolean transferable) {
			this.mosaic = createMosaic(creator, quantity, divisibility, mutableQuantity, transferable);
			this.setupCache();
		}

		private void setupCache() {
			Mockito.when(this.stateCache.findStateByAddress(SIGNER.getAddress())).thenReturn(this.state);
			Mockito.when(this.state.getSmartTileMap()).thenReturn(this.map);
			Mockito.when(this.state.getAccountInfo()).thenReturn(this.accountInfo);
			Mockito.when(this.accountInfo.getBalance()).thenReturn(Amount.fromNem(1));
			Mockito.when(this.namespaceCache.get(NamespaceConstants.NAMESPACE_ID_NEM)).thenReturn(NamespaceConstants.NAMESPACE_ENTRY_NEM);
		}

		private void addMosaicToCache(final Mosaic mosaic) {
			Mockito.when(this.namespaceCache.get(this.namespaceId)).thenReturn(this.namespaceEntry);
			Mockito.when(this.namespaceEntry.getMosaics()).thenReturn(this.mosaics);
			Mockito.when(this.mosaics.get(this.mosaicId)).thenReturn(this.mosaicEntry);
			Mockito.when(this.mosaicEntry.getMosaic()).thenReturn(mosaic);
		}

		private void addSmartTile() {
			this.map.add(createSmartTile("foo", 100));
		}

		private ValidationResult validate(final TransferTransaction transaction) {
			return this.validator.validate(transaction, new ValidationContext(DebitPredicates.Throw));
		}
	}
}
