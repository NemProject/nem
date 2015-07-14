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
	public void validTransactionValidates() {
		// Arrange:
		final TestContext context = new TestContext();
		final TransferTransaction transaction = createTransaction(Amount.fromNem(1), Collections.singletonList(createSmartTile("foo", 10)));
		context.addMosaicToCache(context.mosaic);
		context.addSmartTile();

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
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
		return new SmartTile(new MosaicId(new NamespaceId(namespace), "bar"), Quantity.fromValue(quantity));
	}

	private class TestContext {
		final Mosaic mosaic;
		final AccountStateCache stateCache = Mockito.mock(AccountStateCache.class);
		final MosaicCache mosaicCache = Mockito.mock(MosaicCache.class);
		final AccountState state = Mockito.mock(AccountState.class);
		final SmartTileMap map = new SmartTileMap();
		final SmartTileBagValidator validator = new SmartTileBagValidator(this.stateCache, this.mosaicCache);

		private TestContext() {
			this(SIGNER, 1000, 6, true, true);
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
		}

		private void addMosaicToCache(final Mosaic mosaic) {
			Mockito.when(this.mosaicCache.get(mosaic.getId())).thenReturn(mosaic);
		}

		private void addSmartTile() {
			this.map.add(createSmartTile("foo", 100));
		}

		private ValidationResult validate(final TransferTransaction transaction) {
			return this.validator.validate(transaction, new ValidationContext(DebitPredicates.Throw));
		}
	}
}
